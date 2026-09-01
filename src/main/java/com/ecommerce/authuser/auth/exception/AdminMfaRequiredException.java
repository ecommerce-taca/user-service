package com.ecommerce.authuser.auth.exception;

public class AdminMfaRequiredException extends RuntimeException {

    public AdminMfaRequiredException() {
        super("Admin MFA is required");
    }
}
