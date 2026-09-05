package com.ecommerce.authuser.rbac.application.admin.roles.assignment;

import com.ecommerce.authuser.auth.exception.MfaStepUpRequiredException;
import com.ecommerce.authuser.auth.exception.mfa.MfaAuthenticationRequiredException;
import com.ecommerce.authuser.auth.security.TokenHasher;

import com.ecommerce.authuser.mfa.domain.MfaChallenge;
import com.ecommerce.authuser.mfa.domain.MfaPurpose;
import com.ecommerce.authuser.mfa.domain.MfaStepUpToken;
import com.ecommerce.authuser.mfa.domain.TwoFactorCredential;
import com.ecommerce.authuser.mfa.domain.TwoFactorStatus;

import com.ecommerce.authuser.mfa.repository.MfaChallengeRepository;
import com.ecommerce.authuser.mfa.repository.MfaStepUpTokenRepository;
import com.ecommerce.authuser.mfa.repository.TwoFactorCredentialRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminRbacStepUpService {

    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);

    private static final int MAX_RAW_TOKEN_LENGTH = 512;

    private final UserRepository userRepository;

    private final TwoFactorCredentialRepository credentialRepository;

    private final MfaChallengeRepository challengeRepository;

    private final MfaStepUpTokenRepository stepUpTokenRepository;

    private final TokenHasher tokenHasher;

    @Transactional(
            noRollbackFor =
                    MfaStepUpRequiredException.class
    )
    public void require(
            UUID userId,
            UUID sessionId,
            String rawStepUpToken,
            Instant now
    ) {

        if (userId == null || sessionId == null || now == null) {
            throw new MfaAuthenticationRequiredException();
        }

        User user = userRepository
                .findByIdForUpdate(userId)
                .orElseThrow(MfaAuthenticationRequiredException::new);

        if (hasValidToken(
                userId,
                sessionId,
                rawStepUpToken,
                now
        )) {
            return;
        }

        issueChallenge(
                user,
                sessionId,
                now
        );
    }

    private boolean hasValidToken(
            UUID userId,
            UUID sessionId,
            String rawStepUpToken,
            Instant now
    ) {

        if (rawStepUpToken == null
                || rawStepUpToken.isBlank()
                || rawStepUpToken.length() > MAX_RAW_TOKEN_LENGTH)
        {
            return false;
        }

        String tokenHash = tokenHasher.hash(rawStepUpToken);

        MfaStepUpToken token =
                stepUpTokenRepository
                        .findForValidation(
                                tokenHash,
                                userId,
                                sessionId
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

        if (challenge == null
                || challenge.getPurpose() != MfaPurpose.STEP_UP
                || !challenge.isVerified()
                || challenge.isRevoked()
                || !challenge.belongsToSession(sessionId)
        ) {
            token.revoke(now);
            return false;
        }

        return true;
    }

    private void issueChallenge(
            User user,
            UUID sessionId,
            Instant now
    ) {

        TwoFactorCredential credential = credentialRepository
                .findByUserIdForUpdate(user.getId())
                .orElse(null);

        if (credential == null
                || credential.getStatus() != TwoFactorStatus.ENABLED) {
            throw new MfaStepUpRequiredException(
                    null,
                    null,
                    List.of()
            );
        }

        List<MfaChallenge> active =
                challengeRepository
                        .findActiveForSessionForUpdate(
                                user.getId(),
                                MfaPurpose.STEP_UP,
                                sessionId,
                                now
                        );

        for (MfaChallenge challenge : active) {
            challenge.revoke(now);
        }

        Instant expiresAt = now.plus(CHALLENGE_TTL);

        MfaChallenge challenge =
                MfaChallenge.createStepUp(
                        user,
                        sessionId,
                        now,
                        expiresAt
                );

        challengeRepository.save(challenge);

        throw new MfaStepUpRequiredException(
                challenge.getId(),
                expiresAt,
                List.of(
                        "TOTP",
                        "RECOVERY_CODE"
                )
        );
    }
}
