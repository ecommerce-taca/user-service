package com.ecommerce.authuser.auth.exception.verification.email;

public class InvalidVerificationTokenException extends RuntimeException {

    public InvalidVerificationTokenException() {
        super("Invalid verification token");
    }
}
