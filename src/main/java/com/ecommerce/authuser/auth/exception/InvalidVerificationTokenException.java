package com.ecommerce.authuser.auth.exception;

public class InvalidVerificationTokenException extends RuntimeException {

    public InvalidVerificationTokenException() {
        super("Invalid verification token");
    }
}
