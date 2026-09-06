package com.ecommerce.authuser.auth.application.verification.email;

import java.time.Instant;
import java.util.UUID;

public record EmailVerificationResult(
        UUID userId,
        Instant verifiedAt
) {
}