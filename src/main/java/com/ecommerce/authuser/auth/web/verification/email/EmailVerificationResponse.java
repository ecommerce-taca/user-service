package com.ecommerce.authuser.auth.web.verification.email;

import com.ecommerce.authuser.common.web.RequestMeta;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record EmailVerificationResponse(
        Data data,
        RequestMeta meta
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
}
