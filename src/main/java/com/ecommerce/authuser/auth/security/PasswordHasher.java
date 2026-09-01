package com.ecommerce.authuser.auth.security;

public interface PasswordHasher {
    
    String hash(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}