package com.ecommerce.authuser.rbac.exception;

public class RoleAssignmentNotFoundException extends RuntimeException {

    public RoleAssignmentNotFoundException() {
        super("Role assignment not found");
    }
}
