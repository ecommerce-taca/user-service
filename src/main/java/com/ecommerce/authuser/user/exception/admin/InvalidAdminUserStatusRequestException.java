package com.ecommerce.authuser.user.exception.admin;

public class InvalidAdminUserStatusRequestException extends RuntimeException {

    public InvalidAdminUserStatusRequestException() {
        super("Invalid admin user status request");
    }
}
