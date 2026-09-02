package com.ecommerce.authuser.auth.exception.password;

public class InvalidPasswordResetTokenException extends RuntimeException {

    public InvalidPasswordResetTokenException() {
        super("Invalid password reset token");
    }
}
