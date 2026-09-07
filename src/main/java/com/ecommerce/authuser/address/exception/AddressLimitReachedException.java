package com.ecommerce.authuser.address.exception;

public class AddressLimitReachedException extends RuntimeException {

    public AddressLimitReachedException() {
        super("Address limit reached");
    }
}
