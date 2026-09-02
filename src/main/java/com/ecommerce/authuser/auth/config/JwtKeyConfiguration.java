package com.ecommerce.authuser.auth.config;

import com.ecommerce.authuser.auth.security.AccessSessionTokenValidator;
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

import java.util.Base64;

@Configuration
@EnableConfigurationProperties(JwtKeyProperties.class)
public class JwtKeyConfiguration {

    private static final int MIN_RSA_BITS = 2048;

    @Bean
    public KeyPair jwtKeyPair(JwtKeyProperties properties) {
        try {

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
            AccessSessionTokenValidator accessSessionTokenValidator
    ) {

        RSAPublicKey publicKey =
                (RSAPublicKey) jwtKeyPair.getPublic();

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder
                        .withPublicKey(publicKey)
                        .build();

        OAuth2TokenValidator<Jwt>
                issuerValidator =
                JwtValidators
                        .createDefaultWithIssuer("auth-user-service");

        OAuth2TokenValidator<Jwt>
                audienceValidator =
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

            return Base64
                    .getDecoder()
                    .decode(encoded);

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
                .equals(publicKey.getModulus()
                )) {

            throw new IllegalStateException(
                    "JWT private/public keys do not match"
            );
        }

        if (publicKey.getModulus().bitLength() < MIN_RSA_BITS) {
            throw new IllegalStateException(
                    "JWT RSA key must be at least "
                            + MIN_RSA_BITS
                            + " bits"
            );
        }
    }
}