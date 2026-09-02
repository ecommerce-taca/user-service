package com.ecommerce.authuser.auth.exception;

public class OtpRateLimitedException extends RuntimeException {

    public OtpRateLimitedException() {
        super("OTP request rate limited");
    }
}