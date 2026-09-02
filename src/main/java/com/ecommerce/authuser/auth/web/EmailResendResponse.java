package com.ecommerce.authuser.auth.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record EmailResendResponse(
        Data data,
        Meta meta
) {

    public record Data(
            boolean accepted,

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