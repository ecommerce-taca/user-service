package com.ecommerce.authuser.auth.exception.verification.email;

public class ResendLimitExceededException extends RuntimeException {

    public ResendLimitExceededException() {
        super("Verification resend limit exceeded");
    }
}