package com.ecommerce.authuser.auth.security;

public interface TokenHasher {

    String hash(String rawToken);
}
