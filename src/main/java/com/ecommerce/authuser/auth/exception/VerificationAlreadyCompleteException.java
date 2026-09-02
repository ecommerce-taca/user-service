package com.ecommerce.authuser.auth.exception;

public class VerificationAlreadyCompleteException extends RuntimeException {

    public VerificationAlreadyCompleteException() {

        super("Verification is already complete");
    }
}