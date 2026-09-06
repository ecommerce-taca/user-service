package com.ecommerce.authuser.user.exception.admin;

public class AdminUserStatusConflictException extends RuntimeException {

    public AdminUserStatusConflictException() {
        super("Admin user status change conflicts with current state");
    }
}