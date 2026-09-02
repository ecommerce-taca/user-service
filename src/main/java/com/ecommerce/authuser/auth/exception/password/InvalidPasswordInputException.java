package com.ecommerce.authuser.auth.exception.password;

public class InvalidPasswordInputException extends RuntimeException {

    public InvalidPasswordInputException() {
        super("Invalid password");
    }
}
