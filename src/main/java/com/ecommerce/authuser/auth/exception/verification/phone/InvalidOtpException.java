package com.ecommerce.authuser.auth.exception.verification.phone;

public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException() {
        super("Invalid OTP");
    }
}
