package com.ecommerce.authuser.auth.application.session;

public record RefreshCommand(
        String refreshToken,
        String clientIp
) {
}
