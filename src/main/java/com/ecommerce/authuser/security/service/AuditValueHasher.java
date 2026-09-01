package com.ecommerce.authuser.security.service;

public interface AuditValueHasher {
    String hash(String value);
}