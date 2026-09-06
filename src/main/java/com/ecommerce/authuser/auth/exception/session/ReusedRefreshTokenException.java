package com.ecommerce.authuser.auth.exception.session;

public class ReusedRefreshTokenException extends RuntimeException {

    public ReusedRefreshTokenException() {
        super("Refresh token reuse detected");
    }
}