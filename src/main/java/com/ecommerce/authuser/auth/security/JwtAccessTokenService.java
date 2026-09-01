package com.ecommerce.authuser.auth.security;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtAccessTokenService implements AccessTokenService {

    private static final String ISSUER = "auth-user-service";

    private static final String AUDIENCE = "taca-api";

    private final JwtEncoder jwtEncoder;

    @Override
    public String issue(
            UUID userId,
            UUID sessionId,
            List<String> roles,
            boolean emailVerified,
            Instant issuedAt,
            Instant expiresAt
    ) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(roles);
        Objects.requireNonNull(issuedAt);
        Objects.requireNonNull(expiresAt);

        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after issuedAt"
            );
        }

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(ISSUER)
                        .audience(List.of(AUDIENCE))
                        .subject(userId.toString())
                        .issuedAt(issuedAt)
                        .expiresAt(expiresAt)
                        .id(UuidV7Generator.generate().toString())
                        .claim("session_id", sessionId.toString())
                        .claim("roles", List.copyOf(roles))
                        .claim("email_verified", emailVerified)
                        .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }
}
