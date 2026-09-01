package com.ecommerce.authuser.auth.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record EmailVerificationResponse(
        Data data,
        Meta meta
) {

    public record Data(

            @JsonProperty("user_id")
            UUID userId,

            @JsonProperty("email_verified")
            boolean emailVerified,

            @JsonProperty("verified_at")
            Instant verifiedAt
    ) {
    }

    public record Meta(

            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
