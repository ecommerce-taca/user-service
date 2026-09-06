package com.ecommerce.authuser.auth.exception.verification.phone;

public class OtpAttemptsExceededException extends RuntimeException {

    public OtpAttemptsExceededException() {
        super("Maximum OTP attempts exceeded");
    }
}