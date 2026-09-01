package com.ecommerce.authuser.auth.application;

import java.time.Instant;
import java.util.UUID;

public record SignupResult(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String accessToken,
        String refreshToken,
        long accessExpiresIn,
        long refreshExpiresIn,
        Instant verificationExpiresAt
) {
}
