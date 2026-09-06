package com.ecommerce.authuser.auth.exception.signin;

public class AccountSuspendedException extends RuntimeException {

    public AccountSuspendedException() {
        super("Account is suspended");
    }
}
