package com.ecommerce.authuser.auth.application;

import com.ecommerce.authuser.audit.domain.AuditLog;
import com.ecommerce.authuser.audit.domain.AuditTargetType;
import com.ecommerce.authuser.audit.repository.AuditLogRepository;

import com.ecommerce.authuser.auth.exception.InvalidRefreshTokenException;
import com.ecommerce.authuser.auth.exception.MfaStepUpRequiredException;
import com.ecommerce.authuser.auth.security.TokenHasher;

import com.ecommerce.authuser.mfa.domain.*;
import com.ecommerce.authuser.mfa.repository.MfaChallengeRepository;
import com.ecommerce.authuser.mfa.repository.MfaStepUpTokenRepository;
import com.ecommerce.authuser.mfa.repository.TwoFactorCredentialRepository;
import com.ecommerce.authuser.security.service.AuditValueHasher;

import com.ecommerce.authuser.token.domain.RefreshToken;
import com.ecommerce.authuser.token.domain.TokenRevokeReason;
import com.ecommerce.authuser.token.repository.RefreshTokenLookup;
import com.ecommerce.authuser.token.repository.RefreshTokenRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignoutService {

    private static final Duration MFA_STEP_UP_TTL = Duration.ofMinutes(5);

    private final RefreshTokenRepository refreshTokenRepository;

    private final TokenHasher tokenHasher;

    private final AuditLogRepository auditLogRepository;

    private final AuditValueHasher auditValueHasher;

    private final UserRepository userRepository;

    private final TwoFactorCredentialRepository twoFactorCredentialRepository;

    private final MfaChallengeRepository mfaChallengeRepository;

    private final MfaStepUpTokenRepository mfaStepUpTokenRepository;

    @Transactional(
            noRollbackFor =
                    MfaStepUpRequiredException.class
    )
    public void signout(SignoutCommand command) {
        Instant now = Instant.now();

        if (command.allSessions()) {
            signoutAllSessions(command, now);

            return;
        }


        if (command.refreshToken() != null && !command.refreshToken().isBlank()) {

            String tokenHash = tokenHasher.hash(command.refreshToken());

            RefreshTokenLookup lookup =
                    refreshTokenRepository
                            .findLookupByTokenHash(tokenHash)
                            .orElseThrow(InvalidRefreshTokenException::new);

            if (!lookup.userId().equals(command.userId())
                    || !lookup.familyId().equals(command.sessionId())) {
                throw new InvalidRefreshTokenException();
            }
        }

        List<RefreshToken> familyTokens =
                refreshTokenRepository.findAllByFamilyIdForUpdate(command.sessionId());

        boolean wrongUser =
                familyTokens.stream()
                        .anyMatch(token ->
                                !token.getUser()
                                        .getId()
                                        .equals(command.userId())
                        );

        if (wrongUser) {
            throw new InvalidRefreshTokenException();
        }

        long revokedCount =
                familyTokens.stream()
                        .filter(token ->
                                !token.isRevoked()
                        )
                        .peek(token ->
                                token.revoke(
                                        TokenRevokeReason.SIGNOUT,
                                        now
                                )
                        )
                        .count();

        String clientIp =
                command.clientIp() == null
                        || command.clientIp().isBlank()
                        ? "unknown"
                        : command.clientIp().trim();

        String ipHash =
                auditValueHasher.hash(
                        clientIp
                );

        AuditLog audit =
                AuditLog.create(
                        command.userId(),

                        "AUTH_SIGNOUT",

                        AuditTargetType.USER,

                        command.userId(),

                        "User signed out current session",

                        Map.of(
                                "session_id",
                                command.sessionId()
                                        .toString(),

                                "all_sessions",
                                false,

                                "active_tokens_revoked",
                                revokedCount
                        ),

                        ipHash,

                        now
                );

        auditLogRepository.saveAndFlush(
                audit
        );
    }

    private void requireStepUp(
            User user,
            TwoFactorCredential credential,
            UUID sessionId,
            Instant now
    ) {

        if (credential == null || credential.getStatus() != TwoFactorStatus.ENABLED) {
            throw new MfaStepUpRequiredException(
                    null,
                    null,
                    List.of()
            );
        }

        List<MfaChallenge> activeChallenges =
                mfaChallengeRepository
                        .findActiveForSessionForUpdate(
                                user.getId(),
                                MfaPurpose.STEP_UP,
                                sessionId,
                                now
                        );

        activeChallenges.forEach(challenge -> challenge.revoke(now));

        Instant expiresAt = now.plus(MFA_STEP_UP_TTL);

        MfaChallenge challenge =
                MfaChallenge.createStepUp(
                        user,
                        sessionId,
                        now,
                        expiresAt
                );

        mfaChallengeRepository.save(challenge);

        throw new MfaStepUpRequiredException(
                challenge.getId(),
                expiresAt,
                List.of(
                        "TOTP",
                        "RECOVERY_CODE"
                )
        );
    }

    private void signoutAllSessions(
            SignoutCommand command,
            Instant now
    ) {

        User user = userRepository
                .findByIdForUpdate(command.userId())
                .orElseThrow(InvalidRefreshTokenException::new);

        TwoFactorCredential credential = twoFactorCredentialRepository
                .findByUserIdForUpdate(user.getId())
                .orElse(null);

        if (credential == null
                || credential.getStatus() != TwoFactorStatus.ENABLED) {
            requireStepUp(
                    user,
                    credential,
                    command.sessionId(),
                    now
            );

            return;
        }

        if (!hasValidStepUpToken(command, now)) {
            requireStepUp(
                    user,
                    credential,
                    command.sessionId(),
                    now
            );

            return;
        }

        List<RefreshToken> refreshTokens = refreshTokenRepository
                .findAllActiveByUserForUpdate(user.getId());

        long revokedRefreshTokens = refreshTokens.stream()
                .filter(token -> !token.isRevoked())
                .peek(token ->
                                token.revoke(
                                        TokenRevokeReason.SIGNOUT,
                                        now
                                )
                        )
                        .count();

        List<MfaStepUpToken> stepUpTokens = mfaStepUpTokenRepository
                .findAllActiveByUserForUpdate(user.getId());

        long revokedStepUpTokens = stepUpTokens.stream()
                .filter(token -> !token.isRevoked())
                .peek(token -> token.revoke(now))
                .count();

        saveAllSessionsSignoutAudit(
                command,
                revokedRefreshTokens,
                revokedStepUpTokens,
                now
        );
    }

    private boolean hasValidStepUpToken(
            SignoutCommand command,
            Instant now
    ) {
        String rawStepUpToken = command.stepUpToken();

        if (rawStepUpToken == null
                || rawStepUpToken.isBlank()
                || rawStepUpToken.length() > 512) {

            return false;
        }

        String tokenHash = tokenHasher.hash(rawStepUpToken);

        MfaStepUpToken token = mfaStepUpTokenRepository
                .findForValidation(
                        tokenHash,
                        command.userId(),
                        command.sessionId()
                )
                .orElse(null);

        if (token == null) {
            return false;
        }

        if (!token.isUsable(now)) {
            if (!token.isRevoked()) {
                token.revoke(now);
            }

            return false;
        }

        MfaChallenge challenge = token.getChallenge();

        return challenge.getPurpose() == MfaPurpose.STEP_UP
                && challenge.isVerified()
                && !challenge.isRevoked()
                && challenge.belongsToSession(command.sessionId()
        );
    }

    private void saveAllSessionsSignoutAudit(
            SignoutCommand command,
            long revokedRefreshTokens,
            long revokedStepUpTokens,
            Instant now
    ) {

        String clientIp = command.clientIp() == null
                || command.clientIp().isBlank() ? "unknown" : command.clientIp().trim();

        String ipHash = auditValueHasher.hash(clientIp);

        AuditLog audit =
                AuditLog.create(
                        command.userId(),

                        "AUTH_SIGNOUT",

                        AuditTargetType.USER,

                        command.userId(),

                        "User signed out all sessions",

                        Map.of(
                                "session_id",
                                command.sessionId()
                                        .toString(),

                                "all_sessions",
                                true,

                                "refresh_tokens_revoked",
                                revokedRefreshTokens,

                                "step_up_tokens_revoked",
                                revokedStepUpTokens
                        ),

                        ipHash,

                        now
                );

        auditLogRepository.saveAndFlush(audit);
    }
}