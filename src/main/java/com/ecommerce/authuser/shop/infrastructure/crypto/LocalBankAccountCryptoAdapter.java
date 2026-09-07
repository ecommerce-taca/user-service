package com.ecommerce.authuser.shop.infrastructure.crypto;

import com.ecommerce.authuser.shop.port.BankAccountCryptoPort;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

@Component
@Profile("!prod")
public class LocalBankAccountCryptoAdapter implements BankAccountCryptoPort {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int KEY_SIZE_BITS = 256;

    private static final int IV_LENGTH_BYTES = 12;

    private static final int TAG_LENGTH_BITS = 128;

    private static final String KEY_VERSION = "local-ephemeral-v1";

    private final SecureRandom secureRandom = new SecureRandom();

    private final SecretKey secretKey;

    public LocalBankAccountCryptoAdapter() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");

            generator.init(KEY_SIZE_BITS, secureRandom);

            secretKey = generator.generateKey();

        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "Cannot initialize local bank encryption",
                    ex
            );
        }
    }

    @Override
    public EncryptionResult encrypt(String plaintext) {

        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException(
                    "plaintext must not be blank"
            );
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];

            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );

            byte[] encrypted =
                    cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] payload =
                    new byte[iv.length + encrypted.length];

            System.arraycopy(
                    iv,
                    0,
                    payload,
                    0,
                    iv.length
            );

            System.arraycopy(
                    encrypted,
                    0,
                    payload,
                    iv.length,
                    encrypted.length
            );

            return new EncryptionResult(
                    payload,
                    KEY_VERSION
            );

        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "Cannot encrypt bank account",
                    ex
            );
        }
    }
}
