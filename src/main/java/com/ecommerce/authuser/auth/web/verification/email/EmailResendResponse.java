package com.ecommerce.authuser.auth.web.verification.email;

import com.ecommerce.authuser.common.web.RequestMeta;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record EmailResendResponse(
        Data data,
        RequestMeta meta
) {

    public record Data(
            boolean accepted,

            @JsonProperty("expires_at")
            Instant expiresAt
    ) {
    }
}