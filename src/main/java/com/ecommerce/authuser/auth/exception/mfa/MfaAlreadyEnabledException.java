package com.ecommerce.authuser.auth.exception.mfa;

public class MfaAlreadyEnabledException extends RuntimeException {

    public MfaAlreadyEnabledException() {
        super("MFA is already enabled");
    }
}
