package com.ecommerce.authuser.auth.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureOtpGenerator {

    private static final int OTP_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {

        int value = secureRandom.nextInt(OTP_BOUND);

        return "%06d".formatted(value);
    }
}
