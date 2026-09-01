package com.ecommerce.authuser.auth.application;

import com.ecommerce.authuser.audit.domain.AuditLog;
import com.ecommerce.authuser.audit.domain.AuditTargetType;
import com.ecommerce.authuser.audit.repository.AuditLogRepository;

import com.ecommerce.authuser.auth.exception.InvalidRefreshTokenException;
import com.ecommerce.authuser.auth.exception.MfaStepUpRequiredException;
import com.ecommerce.authuser.auth.security.TokenHasher;

import com.ecommerce.authuser.security.service.AuditValueHasher;

import com.ecommerce.authuser.token.domain.RefreshToken;
import com.ecommerce.authuser.token.domain.TokenRevokeReason;
import com.ecommerce.authuser.token.repository.RefreshTokenLookup;
import com.ecommerce.authuser.token.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SignoutService {

    private final RefreshTokenRepository refreshTokenRepository;

    private final TokenHasher tokenHasher;

    private final AuditLogRepository auditLogRepository;

    private final AuditValueHasher auditValueHasher;

    @Transactional
    public void signout(SignoutCommand command) {
        Instant now = Instant.now();

        if (command.allSessions()) {
            throw new MfaStepUpRequiredException();
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
}