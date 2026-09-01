package com.ecommerce.authuser.auth.application;

public record RefreshResult(
        String accessToken,
        String refreshToken,
        long accessExpiresIn,
        long refreshExpiresIn
) {
}
