package com.ecommerce.authuser.auth.exception;

public class ResendLimitExceededException extends RuntimeException {

    public ResendLimitExceededException() {
        super("Verification resend limit exceeded");
    }
}