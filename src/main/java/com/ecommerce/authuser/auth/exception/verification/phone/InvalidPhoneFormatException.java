package com.ecommerce.authuser.auth.exception.verification.phone;

public class InvalidPhoneFormatException extends RuntimeException {

    public InvalidPhoneFormatException() {
        super("Invalid phone format");
    }
}