package com.ecommerce.authuser.auth.exception.session;

public class ExpiredRefreshTokenException extends RuntimeException {
    public ExpiredRefreshTokenException() {
        super("Refresh token expired");
    }
}