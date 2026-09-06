package com.ecommerce.authuser.auth.application.mfa;

import java.time.Instant;

public record MfaStepUpVerifyResult(
        String stepUpToken,
        Instant expiresAt
) {
}
