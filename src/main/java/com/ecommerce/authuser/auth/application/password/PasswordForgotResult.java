package com.ecommerce.authuser.auth.application.password;

public record PasswordForgotResult(
        boolean accepted
) {

    public static PasswordForgotResult success() {
        return new PasswordForgotResult(true);
    }
}