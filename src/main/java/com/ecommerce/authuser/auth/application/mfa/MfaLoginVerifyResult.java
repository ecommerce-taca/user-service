package com.ecommerce.authuser.auth.application.mfa;

public record MfaLoginVerifyResult(
        String accessToken,
        String refreshToken,
        long accessExpiresIn,
        long refreshExpiresIn
) {
}