package com.ecommerce.authuser.rbac.exception;

public class AdminRbacPermissionDeniedException extends RuntimeException {

    public AdminRbacPermissionDeniedException() {
        super("Admin RBAC permission denied");
    }
}
