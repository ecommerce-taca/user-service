package com.ecommerce.authuser.auth.exception;

public class AccountSuspendedException extends RuntimeException {

    public AccountSuspendedException() {
        super("Account is suspended");
    }
}
