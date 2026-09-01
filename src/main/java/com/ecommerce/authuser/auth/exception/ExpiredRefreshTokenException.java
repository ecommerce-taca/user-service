package com.ecommerce.authuser.auth.exception;

public class ExpiredRefreshTokenException extends RuntimeException {
    public ExpiredRefreshTokenException() {
        super("Refresh token expired");
    }
}