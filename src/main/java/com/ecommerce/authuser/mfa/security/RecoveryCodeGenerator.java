package com.ecommerce.authuser.mfa.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

import java.util.Base64;

@Component
public class RecoveryCodeGenerator {

    private static final int RANDOM_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[RANDOM_BYTES];

        secureRandom.nextBytes(bytes);

        try {
            return "rc_"
                    + Base64
                    .getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(bytes);

        } finally {
            java.util.Arrays.fill(bytes, (byte) 0);
        }
    }
}
