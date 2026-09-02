package com.ecommerce.authuser.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.jwt")
public record JwtKeyProperties(
        String privateKeyBase64,
        String publicKeyBase64
) {
}
