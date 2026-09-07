package com.ecommerce.authuser.user.exception.profile;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("User not found");
    }
}
