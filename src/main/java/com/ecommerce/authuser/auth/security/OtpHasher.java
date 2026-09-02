package com.ecommerce.authuser.auth.security;

public interface OtpHasher {
    String hash(String rawOtp);
}