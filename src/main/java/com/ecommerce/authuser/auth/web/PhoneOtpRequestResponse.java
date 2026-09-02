package com.ecommerce.authuser.auth.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record PhoneOtpRequestResponse(
        Data data,
        Meta meta
) {

    public record Data(
            @JsonProperty("challenge_id")
            UUID challengeId,

            @JsonProperty("masked_phone")
            String maskedPhone,

            @JsonProperty("expires_at")
            Instant expiresAt,

            @JsonProperty("max_attempts")
            int maxAttempts
    ) {
    }

    public record Meta(

            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
