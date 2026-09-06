package com.ecommerce.authuser.auth.config;

import com.ecommerce.authuser.auth.security.AccessSessionTokenValidator;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;

import org.springframework.security.oauth2.jwt.*;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableConfigurationProperties(JwtKeyProperties.class)
public class JwtKeyConfiguration {

    private static final int MIN_RSA_BITS = 2048;

    @Bean
    public KeyPair jwtKeyPair(JwtKeyProperties properties) {
        try {
            requireConfigured(
                    properties.keyId(),
                    "auth.jwt.key-id"
            );

            String privateKeyBase64 =
                    requireConfigured(
                            properties.privateKeyBase64(),
                            "auth.jwt.private-key-base64"
                    );

            String publicKeyBase64 =
                    requireConfigured(
                            properties.publicKeyBase64(),
                            "auth.jwt.public-key-base64"
                    );

            byte[] privateKeyBytes = decodeBase64(privateKeyBase64, "private key");

            byte[] publicKeyBytes =
                    decodeBase64(publicKeyBase64, "public key");

            KeyFactory keyFactory =
                    KeyFactory.getInstance("RSA");

            RSAPrivateKey privateKey =
                    (RSAPrivateKey)
                            keyFactory.generatePrivate(
                                    new PKCS8EncodedKeySpec(privateKeyBytes)
                            );

            RSAPublicKey publicKey =
                    (RSAPublicKey)
                            keyFactory.generatePublic(
                                    new X509EncodedKeySpec(
                                            publicKeyBytes
                                    )
                            );

            validateKeyPair(privateKey, publicKey);

            return new KeyPair(publicKey, privateKey);

        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "Invalid JWT RSA key configuration",
                    ex
            );
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(KeyPair jwtKeyPair) {

        RSAPublicKey publicKey =
                (RSAPublicKey) jwtKeyPair.getPublic();

        RSAPrivateKey privateKey =
                (RSAPrivateKey) jwtKeyPair.getPrivate();

        return NimbusJwtEncoder
                .withKeyPair(publicKey, privateKey)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            KeyPair jwtKeyPair,
            JwtKeyProperties properties,
            AccessSessionTokenValidator accessSessionTokenValidator
    ) {

        RSAPublicKey currentPublicKey =
                (RSAPublicKey)
                        jwtKeyPair.getPublic();

        String currentKeyId =
                requireConfigured(
                        properties.keyId(),
                        "auth.jwt.key-id"
                );

        List<JWK> verificationKeys = new ArrayList<>();

        verificationKeys.add(
                toVerificationJwk(
                        currentKeyId,
                        currentPublicKey
                )
        );

        addPreviousVerificationKey(
                verificationKeys,
                properties,
                currentKeyId
        );

        JWKSource<SecurityContext> jwkSource =
                new ImmutableJWKSet<>(
                        new JWKSet(verificationKeys)
                );

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSource(jwkSource)
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators
                        .createDefaultWithIssuer("auth-user-service");

        OAuth2TokenValidator<Jwt> audienceValidator =
                jwt -> {

                    if (jwt.getAudience().contains("taca-api")) {
                        return OAuth2TokenValidatorResult.success();
                    }

                    OAuth2Error error =
                            new OAuth2Error(
                                    "invalid_token",
                                    "Required audience is missing",
                                    null
                            );

                    return OAuth2TokenValidatorResult
                            .failure(error);
                };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        audienceValidator,
                        accessSessionTokenValidator
                )
        );

        return decoder;
    }

    private static RSAKey toVerificationJwk(
            String keyId,
            RSAPublicKey publicKey
    ) {

        validatePublicKey(publicKey);

        return new RSAKey
                .Builder(publicKey)
                .keyID(keyId)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();
    }

    private static void addPreviousVerificationKey(
            List<JWK> verificationKeys,
            JwtKeyProperties properties,
            String currentKeyId
    ) {

        boolean hasPreviousKeyId = hasText(properties.previousKeyId());

        boolean hasPreviousPublicKey = hasText(properties.previousPublicKeyBase64());

        if (hasPreviousKeyId
                != hasPreviousPublicKey) {

            throw new IllegalStateException(
                    "Previous JWT key configuration is incomplete"
            );
        }

        if (!hasPreviousKeyId) {
            return;
        }

        String previousKeyId =
                properties.previousKeyId()
                        .strip();

        if (currentKeyId.equals(
                previousKeyId
        )) {

            throw new IllegalStateException(
                    "Current and previous JWT key ids must differ"
            );
        }

        RSAPublicKey previousPublicKey =
                decodePublicKey(
                        properties
                                .previousPublicKeyBase64()
                );

        verificationKeys.add(
                toVerificationJwk(
                        previousKeyId,
                        previousPublicKey
                )
        );
    }

    private static RSAPublicKey decodePublicKey(String encoded) {
        try {
            byte[] publicKeyBytes =
                    decodeBase64(encoded, "previous public key");

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return (RSAPublicKey)
                    keyFactory.generatePublic(
                            new X509EncodedKeySpec(publicKeyBytes)
                    );

        } catch (GeneralSecurityException | ClassCastException ex) {
            throw new IllegalStateException(
                    "Invalid previous JWT RSA public key",
                    ex
            );
        }
    }

    private static String requireConfigured(
            String value,
            String property
    ) {

        if (value == null || value.isBlank()) {

            throw new IllegalStateException(
                    property + " must be configured"
            );
        }

        return value.trim();
    }

    private static byte[] decodeBase64(
            String encoded,
            String keyName
    ) {

        try {
            return Base64.getDecoder().decode(encoded);

        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Invalid Base64 JWT " + keyName,
                    ex
            );
        }
    }

    private static void validateKeyPair(
            RSAPrivateKey privateKey,
            RSAPublicKey publicKey
    ) {

        if (!privateKey
                .getModulus()
                .equals(publicKey.getModulus())) {

            throw new IllegalStateException(
                    "JWT private/public keys do not match"
            );
        }

        validatePublicKey(publicKey);
    }

    private static void validatePublicKey(
            RSAPublicKey publicKey
    ) {

        if (publicKey.getModulus().bitLength() < MIN_RSA_BITS) {
            throw new IllegalStateException(
                    "JWT RSA key must be at least "
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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}