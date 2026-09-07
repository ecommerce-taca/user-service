package com.ecommerce.authuser.user.exception.profile;

public class ProfilePhoneAlreadyExistsException extends RuntimeException {

    public ProfilePhoneAlreadyExistsException() {
        super("Phone already exists");
    }
}
