package com.ecommerce.authuser.auth.exception.mfa;


public class InvalidMfaCodeException extends RuntimeException {

    public InvalidMfaCodeException() {
        super("Invalid MFA code");
    }
}
