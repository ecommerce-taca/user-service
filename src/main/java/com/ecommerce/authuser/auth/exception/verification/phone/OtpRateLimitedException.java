package com.ecommerce.authuser.auth.exception.verification.phone;

public class OtpRateLimitedException extends RuntimeException {

    public OtpRateLimitedException() {
        super("OTP request rate limited");
    }
}