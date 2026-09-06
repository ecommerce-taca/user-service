package com.ecommerce.authuser.rbac.exception;

public class RoleAssignmentExistsException extends RuntimeException {

    public RoleAssignmentExistsException() {
        super("Role assignment already exists");
    }
}
