package com.ecommerce.authuser.audit.exception;

public class InvalidAdminAuditQueryException extends RuntimeException {

    public InvalidAdminAuditQueryException() {
        super("Invalid admin audit query");
    }
}