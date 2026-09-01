package com.ecommerce.authuser.auth.exception;

public class MfaStepUpRequiredException extends RuntimeException {

    public MfaStepUpRequiredException() {
        super("Step-up authentication is required");
    }
}
