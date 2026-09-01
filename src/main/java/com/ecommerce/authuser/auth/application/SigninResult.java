package com.ecommerce.authuser.auth.application;

import java.util.List;
import java.util.UUID;

public record SigninResult(
        UUID userId,
        String fullName,
        String email,
        boolean emailVerified,
        String phone,
        boolean phoneVerified,
        List<String> roles,
        String status,
        String accessToken,
        String refreshToken,
        long accessExpiresIn,
        long refreshExpiresIn
) {
}
