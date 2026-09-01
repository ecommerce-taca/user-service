package com.ecommerce.authuser.auth.exception;

public class PhoneAlreadyExistsException extends RuntimeException {

    public PhoneAlreadyExistsException() {
        super("Phone already exists");
    }
}
