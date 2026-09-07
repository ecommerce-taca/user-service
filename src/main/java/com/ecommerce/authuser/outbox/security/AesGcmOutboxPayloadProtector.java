package com.ecommerce.authuser.outbox.security;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Component
public class AesGcmOutboxPayloadProtector
        implements OutboxPayloadProtector {

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static final int IV_LENGTH = 12;

    private static final int TAG_LENGTH_BITS = 128;

    private final ObjectMapper objectMapper;

    private final SecretKeySpec secretKey;

    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmOutboxPayloadProtector(
            ObjectMapper objectMapper,
            @Value("${auth.outbox.encryption-key-base64}") String encryptionKeyBase64
    ) {
        this.objectMapper = objectMapper;

        byte[] key = Base64.getDecoder().decode(encryptionKeyBase64);

        if (key.length != 32) {
            throw new IllegalArgumentException(
                    "Outbox encryption key must be 32 bytes"
            );
        }

        this.secretKey = new SecretKeySpec(key, "AES");
    }

    @Override
    public Map<String, Object> protect(
            String context,
            Map<String, Object> payload
    ) {

        try {
            byte[] iv = new byte[IV_LENGTH];

            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );

            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));

            byte[] plaintext = objectMapper.writeValueAsBytes(payload);

            byte[] ciphertext = cipher.doFinal(plaintext);

            return Map.of(
                    "protected", true,

                    "alg",
                    "AES-256-GCM",

                    "key_version",
                    "local-v1",

                    "iv",
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(iv),

                    "ciphertext",
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(ciphertext)
            );

        } catch (GeneralSecurityException | JacksonException ex) {
            throw new IllegalStateException(
                    "Cannot protect outbox payload",
                    ex
            );
        }
    }
}
