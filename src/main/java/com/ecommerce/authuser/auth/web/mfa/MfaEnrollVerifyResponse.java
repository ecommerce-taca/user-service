package com.ecommerce.authuser.auth.web.mfa;

import com.ecommerce.authuser.common.web.RequestMeta;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record MfaEnrollVerifyResponse(
        Data data,
        RequestMeta meta
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
}