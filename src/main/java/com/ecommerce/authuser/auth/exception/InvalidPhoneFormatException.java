package com.ecommerce.authuser.auth.exception;

public class InvalidPhoneFormatException extends RuntimeException {

    public InvalidPhoneFormatException() {
        super("Invalid phone format");
    }
}