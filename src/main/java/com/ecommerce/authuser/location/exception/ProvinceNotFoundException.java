package com.ecommerce.authuser.location.exception;

public class ProvinceNotFoundException extends RuntimeException {

    public ProvinceNotFoundException() {
        super("Province was not found");
    }
}
