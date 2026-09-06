package com.ecommerce.authuser.support.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.RSAKey;

import org.springframework.context.annotation.Primary;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@TestConfiguration(proxyBeanMethods = false)
public class TestSecurityConfig {

        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
                RSAPublicKey publicKey = (RSAPublicKey) TestJwtFactory
                                .getKeyPair()
                                .getPublic();

                NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
                                .build();

                OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(
                                TestJwtFactory.getIssuer());

                OAuth2TokenValidator<Jwt> audienceValidator = new TestAudienceValidator(
                                TestJwtFactory.getAudience());

                decoder.setJwtValidator(
                                new DelegatingOAuth2TokenValidator<>(
                                                issuerValidator,
                                                audienceValidator));

                return decoder;
        }

        @Bean
        @Primary
        JwtEncoder testJwtEncoder() {
                RSAPublicKey publicKey = (RSAPublicKey) TestJwtFactory
                                .getKeyPair()
                                .getPublic();

                RSAPrivateKey privateKey = (RSAPrivateKey) TestJwtFactory
                                .getKeyPair()
                                .getPrivate();

                JWK jwk = new RSAKey.Builder(publicKey)
                                .privateKey(privateKey)
                                .keyID(TestJwtFactory.getKeyId())
                                .keyUse(KeyUse.SIGNATURE)
                                .algorithm(JWSAlgorithm.RS256)
                                .build();

                JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(
                                new JWKSet(jwk));

                return new NimbusJwtEncoder(jwkSource);
        }
}