package com.ecommerce.authuser.address.exception;

public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException() {
        super("Address not found");
    }
}