package com.ecommerce.authuser.auth.exception.mfa;

public class MfaAttemptsExceededException extends RuntimeException {

    public MfaAttemptsExceededException() {
        super("MFA attempts exceeded");
    }
}
