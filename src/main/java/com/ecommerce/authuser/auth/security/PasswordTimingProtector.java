package com.ecommerce.authuser.auth.security;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PasswordTimingProtector {

    private final PasswordHasher passwordHasher;

    private String dummyPasswordHash;

    @PostConstruct
    void initialize() {
        dummyPasswordHash = passwordHasher.hash(UUID.randomUUID().toString());
    }

    public void consume(String rawPassword) {
        passwordHasher.matches(
                rawPassword,
                dummyPasswordHash
        );
    }
}
