package com.ecommerce.authuser.auth.web.verification.phone;

import com.ecommerce.authuser.common.web.RequestMeta;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record PhoneOtpVerifyResponse(
        Data data,
        RequestMeta meta
) {

    public record Data(

            @JsonProperty("phone_verified")
            boolean phoneVerified,

            @JsonProperty("phone_verified_at")
            Instant phoneVerifiedAt
    ) {
    }
}
