package com.ecommerce.authuser.auth.exception.mfa;

public class MfaAuthenticationRequiredException extends RuntimeException {

    public MfaAuthenticationRequiredException() {
        super("Authentication is required");
    }
}
