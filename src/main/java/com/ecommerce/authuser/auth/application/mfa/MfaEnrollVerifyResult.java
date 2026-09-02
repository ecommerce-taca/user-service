package com.ecommerce.authuser.auth.application.mfa;

import java.time.Instant;
import java.util.List;

public record MfaEnrollVerifyResult(
        String status,
        Instant enabledAt,
        List<String> recoveryCodes
) {

    public MfaEnrollVerifyResult {

        recoveryCodes = List.copyOf(recoveryCodes);
    }
}