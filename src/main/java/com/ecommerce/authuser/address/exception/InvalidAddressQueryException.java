package com.ecommerce.authuser.address.exception;

public class InvalidAddressQueryException extends RuntimeException {

    public InvalidAddressQueryException() {
        super("Invalid address query");
    }
}
