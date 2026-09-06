package com.ecommerce.authuser.auth.web.mfa;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record MfaSetupResponse(
        Data data,
        Meta meta
) {

    public record Data(
            @JsonProperty("setup_id")
            UUID setupId,

            String issuer,

            String account,

            @JsonProperty("otpauth_uri")
            String otpauthUri,

            @JsonProperty("expires_at")
            Instant expiresAt
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}