package com.ecommerce.authuser.mfa.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

@Component
public class AesGcmTotpSecretProtector implements TotpSecretProtector {

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static final String KEY_VERSION = "local-v1";

    private static final int IV_LENGTH = 12;

    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;

    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmTotpSecretProtector(
            @Value("${auth.mfa.encryption-key-base64}") String keyBase64
    ) {

        byte[] key = Base64.getDecoder().decode(keyBase64);

        if (key.length != 32) {
            throw new IllegalArgumentException(
                    "MFA encryption key must be 32 bytes"
            );
        }

        this.secretKey = new SecretKeySpec(key, "AES");
    }

    @Override
    public byte[] encrypt(
            UUID userId,
            byte[] rawSecret
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

            cipher.updateAAD(aad(userId));

            byte[] ciphertext = cipher.doFinal(rawSecret);

            return ByteBuffer
                    .allocate(iv.length + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array();

        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "Cannot encrypt TOTP secret",
                    ex
            );
        }
    }

    @Override
    public byte[] decrypt(
            UUID userId,
            byte[] encryptedSecret,
            String keyVersion
    ) {

        if (!KEY_VERSION.equals(keyVersion)) {
            throw new IllegalStateException(
                    "Unsupported MFA key version"
            );
        }

        if (encryptedSecret == null
                || encryptedSecret.length <= IV_LENGTH) {
            throw new IllegalArgumentException(
                    "Invalid encrypted TOTP secret"
            );
        }

        try {
            byte[] iv = Arrays.copyOfRange(
                    encryptedSecret,
                    0,
                    IV_LENGTH
            );

            byte[] ciphertext =
                    Arrays.copyOfRange(
                            encryptedSecret,
                            IV_LENGTH,
                            encryptedSecret.length
                    );

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );

            cipher.updateAAD(aad(userId));

            return cipher.doFinal(ciphertext);

        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "Cannot decrypt TOTP secret",
                    ex
            );
        }
    }

    @Override
    public String keyVersion() {return KEY_VERSION;}

    private byte[] aad(UUID userId) {
        return ("TOTP:" + userId).getBytes(StandardCharsets.UTF_8);
    }
}
