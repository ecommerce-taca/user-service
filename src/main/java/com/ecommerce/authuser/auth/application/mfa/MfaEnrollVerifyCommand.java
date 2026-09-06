package com.ecommerce.authuser.auth.application.mfa;

import java.util.UUID;

public record MfaEnrollVerifyCommand(
        UUID userId,
        UUID setupId,
        String code
) {
}
