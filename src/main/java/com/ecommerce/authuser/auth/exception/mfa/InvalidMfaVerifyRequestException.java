package com.ecommerce.authuser.auth.exception.mfa;

public class InvalidMfaVerifyRequestException extends RuntimeException {

    public InvalidMfaVerifyRequestException() {
        super("Invalid MFA verify request");
    }
}
