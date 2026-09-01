package com.ecommerce.authuser.auth.application;

public record RefreshCommand(
        String refreshToken,
        String clientIp
) {
}
