package com.ecommerce.authuser.support.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.JOSEObjectType;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class TestJwtFactory {

    public static final String ISSUER = "auth-user-service";
    
    public static final String AUDIENCE = "taca-api";

    private static final String KEY_ID = "test-rsa-key";

    private static final KeyPair KEY_PAIR = generateKeyPair();

    private TestJwtFactory() {
    }

    public static TestUserToken createUserToken(
            UUID userId,
            List<String> roles
    ) {
        return createUserToken(
                userId,
                roles,
                Instant.now().plusSeconds(3600)
        );
    }

    public static TestUserToken createUserToken(
            UUID userId,
            List<String> roles,
            Instant expiresAt
    ) {
        Instant issuedAt = Instant.now();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .claim("roles", roles)
                .jwtID(UUID.randomUUID().toString())
                .build();

        try {
            SignedJWT signedJwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(KEY_ID)
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims
            );

            JWSSigner signer = new RSASSASigner(
                    KEY_PAIR.getPrivate()
            );

            signedJwt.sign(signer);

            return new TestUserToken(
                    signedJwt.serialize(),
                    userId
            );

        } catch (JOSEException e) {
            throw new IllegalStateException(
                    "Failed to create test JWT",
                    e
            );
        }
    }

    public static KeyPair getKeyPair() {
        return KEY_PAIR;
    }

    public static String getIssuer() {
        return ISSUER;
    }

    public static String getAudience() {
        return AUDIENCE;
    }

    public static String getKeyId() {
        return KEY_ID;
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator =
                    KeyPairGenerator.getInstance("RSA");

            generator.initialize(2048);

            return generator.generateKeyPair();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "RSA algorithm is not available",
                    e
            );
        }
    }
}
