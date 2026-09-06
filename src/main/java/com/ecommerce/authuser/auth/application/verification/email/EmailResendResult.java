package com.ecommerce.authuser.auth.application.verification.email;

import java.time.Instant;

public record EmailResendResult(
        Instant expiresAt
) {
}