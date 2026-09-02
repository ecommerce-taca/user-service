package com.ecommerce.authuser.auth.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record PhoneOtpVerifyResponse(
        Data data,
        Meta meta
) {

    public record Data(

            @JsonProperty("phone_verified")
            boolean phoneVerified,

            @JsonProperty("phone_verified_at")
            Instant phoneVerifiedAt
    ) {
    }

    public record Meta(

            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
