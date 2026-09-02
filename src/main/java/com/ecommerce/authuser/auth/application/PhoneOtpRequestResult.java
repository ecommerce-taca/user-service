package com.ecommerce.authuser.auth.application;

import java.time.Instant;
import java.util.UUID;

public record PhoneOtpRequestResult(
        UUID challengeId,
        String maskedPhone,
        Instant expiresAt,
        int maxAttempts
) {
}
