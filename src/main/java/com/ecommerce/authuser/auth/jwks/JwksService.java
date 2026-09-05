package com.ecommerce.authuser.auth.jwks;

import com.ecommerce.authuser.auth.config.JwtKeyProperties;

import org.springframework.stereotype.Service;

import java.math.BigInteger;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;

import java.security.interfaces.RSAPublicKey;

import java.security.spec.X509EncodedKeySpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Service
public class JwksService {

    private static final int MIN_RSA_BITS = 2048;

    private static final String KEY_TYPE = "RSA";

    private static final String KEY_USE = "sig";

    private static final String ALGORITHM = "RS256";

    private final JwksResult result;

    public JwksService(
            KeyPair currentKeyPair,
            JwtKeyProperties properties
    ) {

        if (currentKeyPair == null
                || !(currentKeyPair.getPublic()
                instanceof RSAPublicKey currentPublicKey)) {

            throw new IllegalStateException(
                    "Current JWT RSA public key is unavailable"
            );
        }

        String currentKeyId =
                requireText(
                        properties.keyId(),
                        "auth.jwt.key-id"
                );

        validateRsaPublicKey(currentPublicKey);

        List<JwksResult.Key> keys = new ArrayList<>();

        keys.add(
                toJwk(
                        currentKeyId,
                        currentPublicKey
                )
        );

        addPreviousKeyIfConfigured(
                keys,
                properties,
                currentKeyId
        );

        this.result = new JwksResult(keys);
    }

    public JwksResult get() {
        return result;
    }

    private void addPreviousKeyIfConfigured(
            List<JwksResult.Key> keys,
            JwtKeyProperties properties,
            String currentKeyId
    ) {

        boolean hasPreviousKeyId =
                hasText(properties.previousKeyId());

        boolean hasPreviousPublicKey =
                hasText(
                        properties.previousPublicKeyBase64()
                );

        if (hasPreviousKeyId != hasPreviousPublicKey) {
            throw new IllegalStateException(
                    "Previous JWT key configuration is incomplete"
            );
        }

        if (!hasPreviousKeyId) {
            return;
        }

        String previousKeyId = properties.previousKeyId().strip();

        if (currentKeyId.equals(previousKeyId)) {
            throw new IllegalStateException(
                    "Current and previous JWT key ids must differ"
            );
        }

        RSAPublicKey previousPublicKey =
                decodePublicKey(
                        properties
                                .previousPublicKeyBase64()
                );

        validateRsaPublicKey(
                previousPublicKey
        );

        keys.add(
                toJwk(
                        previousKeyId,
                        previousPublicKey
                )
        );
    }

    private JwksResult.Key toJwk(
            String keyId,
            RSAPublicKey publicKey
    ) {

        return new JwksResult.Key(
                KEY_TYPE,
                keyId,
                KEY_USE,
                ALGORITHM,
                base64UrlUnsigned(publicKey.getModulus()),
                base64UrlUnsigned(publicKey.getPublicExponent())
        );
    }

    private RSAPublicKey decodePublicKey(String encoded) {
        try {
            byte[] keyBytes =
                    Base64.getDecoder().decode(encoded.strip());
            KeyFactory keyFactory =
                    KeyFactory.getInstance("RSA");

            return (RSAPublicKey)
                    keyFactory.generatePublic(
                            new X509EncodedKeySpec(keyBytes)
                    );

        } catch (
                IllegalArgumentException
                | GeneralSecurityException
                | ClassCastException ex
        ) {

            throw new IllegalStateException(
                    "Invalid previous JWT RSA public key",
                    ex
            );
        }
    }

    private void validateRsaPublicKey(RSAPublicKey publicKey) {
        if (publicKey.getModulus().bitLength() < MIN_RSA_BITS) {
            throw new IllegalStateException(
                    "JWT RSA public key must be at least "
                            + MIN_RSA_BITS
                            + " bits"
            );
        }

        if (publicKey.getPublicExponent().signum() <= 0) {
            throw new IllegalStateException(
                    "Invalid JWT RSA public exponent"
            );
        }
    }

    private String base64UrlUnsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();

        if (bytes.length > 1 && bytes[0] == 0) {
            bytes =
                    Arrays.copyOfRange(
                            bytes,
                            1,
                            bytes.length
                    );
        }

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String requireText(
            String value,
            String property
    ) {

        if (!hasText(value)) {
            throw new IllegalStateException(
                    property + " must be configured"
            );
        }

        return value.strip();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}