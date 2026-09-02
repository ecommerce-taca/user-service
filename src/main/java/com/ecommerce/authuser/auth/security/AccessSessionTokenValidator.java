package com.ecommerce.authuser.auth.security;

import com.ecommerce.authuser.token.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;

import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccessSessionTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_SESSION =
            new OAuth2Error(
                    "invalid_token",
                    "Session is no longer active",
                    null
            );

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

        UUID userId;
        UUID sessionId;

        try {
            if (jwt.getSubject() == null) {
                return failure();
            }

            String sessionClaim = jwt.getClaimAsString("session_id");

            if (sessionClaim == null || sessionClaim.isBlank()) {
                return failure();
            }

            userId = UUID.fromString(jwt.getSubject());

            sessionId = UUID.fromString(sessionClaim);

        } catch (RuntimeException ex) {
            return failure();
        }

        boolean active = refreshTokenRepository
                .existsByUser_IdAndFamilyIdAndRevokedAtIsNullAndExpiresAtAfter(
                        userId,
                        sessionId,
                        Instant.now()
                );

        if (!active) {
            return failure();
        }

        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult failure() {
        return OAuth2TokenValidatorResult.failure(INVALID_SESSION);
    }
}
