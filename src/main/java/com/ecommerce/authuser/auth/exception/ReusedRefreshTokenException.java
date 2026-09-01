package com.ecommerce.authuser.auth.exception;

public class ReusedRefreshTokenException extends RuntimeException {

    public ReusedRefreshTokenException() {
        super("Refresh token reuse detected");
    }
}