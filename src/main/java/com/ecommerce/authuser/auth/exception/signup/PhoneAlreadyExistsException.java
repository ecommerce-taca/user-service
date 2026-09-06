package com.ecommerce.authuser.auth.exception.signup;

public class PhoneAlreadyExistsException extends RuntimeException {

    public PhoneAlreadyExistsException() {
        super("Phone already exists");
    }
}
