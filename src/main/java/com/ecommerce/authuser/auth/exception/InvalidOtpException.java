package com.ecommerce.authuser.auth.exception;

public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException() {
        super("Invalid OTP");
    }
}
