package com.ecommerce.authuser.auth.exception.password;

public class InvalidPasswordRecoveryInputException
        extends RuntimeException {

    public InvalidPasswordRecoveryInputException() {
        super("Invalid password recovery input");
    }
}