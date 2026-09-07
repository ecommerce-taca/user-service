package com.ecommerce.authuser.address.exception;

public class InvalidAddressInputException extends RuntimeException {

    public InvalidAddressInputException() {
        super("Invalid address input");
    }
}
