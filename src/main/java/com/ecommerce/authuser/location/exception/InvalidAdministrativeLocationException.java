package com.ecommerce.authuser.location.exception;

public class InvalidAdministrativeLocationException extends RuntimeException {

    public InvalidAdministrativeLocationException() {
        super("Administrative location is invalid");
    }
}
