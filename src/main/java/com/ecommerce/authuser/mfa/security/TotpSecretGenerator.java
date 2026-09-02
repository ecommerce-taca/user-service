package com.ecommerce.authuser.mfa.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class TotpSecretGenerator {

    private static final int SECRET_LENGTH = 20;

    private final SecureRandom secureRandom = new SecureRandom();

    public byte[] generate() {
        byte[] secret = new byte[SECRET_LENGTH];

        secureRandom.nextBytes(secret);

        return secret;
    }
}
