package com.ecommerce.authuser.auth.web.mfa;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record MfaEnrollVerifyResponse(
        Data data,
        Meta meta
) {

    public record Data(
            String status,

            @JsonProperty("enabled_at")
            Instant enabledAt,

            @JsonProperty("recovery_codes")
            List<String> recoveryCodes

    ) {
        public Data {recoveryCodes = List.copyOf(recoveryCodes);

        }
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId

    ) {
    }
}