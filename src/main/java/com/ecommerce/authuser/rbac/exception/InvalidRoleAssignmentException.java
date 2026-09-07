package com.ecommerce.authuser.rbac.exception;

public class InvalidRoleAssignmentException extends RuntimeException {

    public InvalidRoleAssignmentException() {
        super("Invalid role assignment");
    }
}
