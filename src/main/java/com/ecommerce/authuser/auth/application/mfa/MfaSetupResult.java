package com.ecommerce.authuser.auth.application.mfa;

import java.time.Instant;
import java.util.UUID;

public record MfaSetupResult(
        UUID setupId,
        String issuer,
        String account,
        String otpauthUri,
        Instant expiresAt
) {
}
