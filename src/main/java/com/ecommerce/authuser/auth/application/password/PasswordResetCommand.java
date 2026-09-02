package com.ecommerce.authuser.auth.application.password;

public record PasswordResetCommand(
        String token,
        String newPassword
) {
}
