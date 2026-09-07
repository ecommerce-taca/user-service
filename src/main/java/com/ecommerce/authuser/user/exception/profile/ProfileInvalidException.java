package com.ecommerce.authuser.user.exception.profile;

public class ProfileInvalidException extends RuntimeException {

    public ProfileInvalidException() {
        super("Invalid profile");
    }
}
