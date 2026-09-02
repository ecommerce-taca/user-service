package com.ecommerce.authuser.address.exception;

public class AddressDefaultRequiredException extends RuntimeException {

    public AddressDefaultRequiredException() {
        super("Default address required");
    }
}
