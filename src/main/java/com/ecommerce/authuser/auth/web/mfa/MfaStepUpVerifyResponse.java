package com.ecommerce.authuser.auth.web.mfa;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record MfaStepUpVerifyResponse(
        Data data,
        Meta meta
) {

    public record Data(

            @JsonProperty("step_up_token")
            String stepUpToken,

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
