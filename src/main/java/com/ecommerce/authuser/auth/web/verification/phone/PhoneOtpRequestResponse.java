package com.ecommerce.authuser.auth.web.verification.phone;

import com.ecommerce.authuser.common.web.RequestMeta;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record PhoneOtpRequestResponse(
        Data data,
        RequestMeta meta
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
}
