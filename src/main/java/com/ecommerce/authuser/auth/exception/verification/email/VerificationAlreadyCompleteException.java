package com.ecommerce.authuser.auth.exception.verification.email;

public class VerificationAlreadyCompleteException extends RuntimeException {

    public VerificationAlreadyCompleteException() {

        super("Verification is already complete");
    }
}