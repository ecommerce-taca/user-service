package com.ecommerce.authuser.kyc.web.admin.review;

import com.ecommerce.authuser.shop.domain.KycStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record AdminKycReviewResponse(
        Data data,
        Meta meta
) {

    public record Data(
            @JsonProperty("shop_id")
            UUID shopId,

            @JsonProperty("kyc_case_id")
            UUID kycCaseId,

            KycStatus status,

            @JsonProperty("reviewed_at")
            Instant reviewedAt
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
