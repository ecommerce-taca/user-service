package com.ecommerce.authuser.outbox.security;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmOutboxPayloadProtectorTest {

    private static final String KEY =
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private final AesGcmOutboxPayloadProtector protector =
            new AesGcmOutboxPayloadProtector(
                    new ObjectMapper(),
                    KEY
            );

    @Test
    void unprotect_shouldRestoreProtectedPayload() {
        Map<String, Object> payload = Map.of(
                "user_id", "user-1",
                "email", "buyer@example.com",
                "status", "ACTIVE"
        );

        Map<String, Object> protectedPayload = protector.protect(
                "user.created",
                payload
        );

        Map<String, Object> result = protector.unprotect(
                "user.created",
                protectedPayload
        );

        assertThat(result).containsEntry("user_id", "user-1");
        assertThat(result).containsEntry("email", "buyer@example.com");
        assertThat(result).containsEntry("status", "ACTIVE");
    }

    @Test
    void unprotect_shouldReturnPlainPayloadWhenPayloadIsNotProtected() {
        Map<String, Object> payload = Map.of(
                "user_id", "user-1"
        );

        Map<String, Object> result = protector.unprotect(
                "user.created",
                payload
        );

        assertThat(result).containsEntry("user_id", "user-1");
    }

    @Test
    void unprotect_shouldRejectWrongContext() {
        Map<String, Object> protectedPayload = protector.protect(
                "user.created",
                Map.of("user_id", "user-1")
        );

        assertThatThrownBy(() -> protector.unprotect(
                "user.updated",
                protectedPayload
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unprotect_shouldRejectMissingCiphertext() {
        Map<String, Object> invalidPayload = Map.of(
                "protected", true,
                "iv", "abc"
        );

        assertThatThrownBy(() -> protector.unprotect(
                "user.created",
                invalidPayload
        )).isInstanceOf(IllegalArgumentException.class);
    }
}