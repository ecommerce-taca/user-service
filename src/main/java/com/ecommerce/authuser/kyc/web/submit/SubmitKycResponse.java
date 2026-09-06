package com.ecommerce.authuser.kyc.web.submit;

import com.ecommerce.authuser.shop.domain.KycStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record SubmitKycResponse(
        Data data,
        Meta meta
) {

    public record Data(
            @JsonProperty("shop_id")
            UUID shopId,

            @JsonProperty("kyc_case_id")
            UUID kycCaseId,

            KycStatus status,

            @JsonProperty("submitted_at")
            Instant submittedAt
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
