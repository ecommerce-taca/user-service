package com.ecommerce.authuser.auth.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2PasswordHasher implements PasswordHasher {

    private final Argon2PasswordEncoder encoder =
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(
            String rawPassword,
            String encodedPassword
    ) {
        return encoder.matches(
                rawPassword,
                encodedPassword
        );
    }
}
