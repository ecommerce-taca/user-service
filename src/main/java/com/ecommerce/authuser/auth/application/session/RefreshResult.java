package com.ecommerce.authuser.auth.application.session;

public record RefreshResult(
        String accessToken,
        String refreshToken,
        long accessExpiresIn,
        long refreshExpiresIn
) {
}
