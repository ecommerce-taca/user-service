package com.ecommerce.authuser.auth.web.mfa;

import com.ecommerce.authuser.common.web.RequestMeta;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record MfaStepUpVerifyResponse(
        Data data,
        RequestMeta meta
) {

    public record Data(

            @JsonProperty("step_up_token")
            String stepUpToken,

            @JsonProperty("expires_at")
            Instant expiresAt
    ) {
    }
}
