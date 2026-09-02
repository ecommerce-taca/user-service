package com.ecommerce.authuser.auth.exception;

public class OtpAttemptsExceededException extends RuntimeException {

    public OtpAttemptsExceededException() {
        super("Maximum OTP attempts exceeded");
    }
}