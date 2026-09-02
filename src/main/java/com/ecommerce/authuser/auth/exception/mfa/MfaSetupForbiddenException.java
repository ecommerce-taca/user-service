package com.ecommerce.authuser.auth.exception.mfa;

public class MfaSetupForbiddenException extends RuntimeException {

    public MfaSetupForbiddenException() {
        super("MFA setup is not allowed");
    }
}
