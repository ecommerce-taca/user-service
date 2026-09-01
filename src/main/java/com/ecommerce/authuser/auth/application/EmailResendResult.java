package com.ecommerce.authuser.auth.application;

import java.time.Instant;

public record EmailResendResult(
        Instant expiresAt
) {
}