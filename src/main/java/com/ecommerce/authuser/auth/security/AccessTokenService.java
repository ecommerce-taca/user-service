package com.ecommerce.authuser.auth.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AccessTokenService {

    String issue(
            UUID userId,
            UUID sessionId,
            List<String> roles,
            boolean emailVerified,
            Instant issuedAt,
            Instant expiresAt
    );
}
