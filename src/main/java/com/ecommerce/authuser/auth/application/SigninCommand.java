package com.ecommerce.authuser.auth.application;

public record SigninCommand(
        String identifier,
        String password,
        boolean rememberMe,
        String clientIp,
        String userAgent
) {
}
