package com.ecommerce.authuser.auth.application.signin;

public record SigninCommand(
        String identifier,
        String password,
        boolean rememberMe,
        String clientIp,
        String userAgent
) {
}
