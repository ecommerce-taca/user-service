package com.ecommerce.authuser.kyc.web.admin.queue;

import com.ecommerce.authuser.shop.domain.KycStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminKycQueueResponse(
        List<Item> data,
        Meta meta
) {

    public record Item(
            @JsonProperty("shop_id")
            UUID shopId,

            @JsonProperty("shop_name")
            String shopName,

            @JsonProperty("owner_user_id")
            UUID ownerUserId,

            @JsonProperty("kyc_case_id")
            UUID kycCaseId,

            KycStatus status,

            @JsonProperty("submitted_at")
            Instant submittedAt,

            @JsonProperty("document_count")
            long documentCount,

            @JsonProperty("age_hours")
            double ageHours
    ) {
    }

    public record Meta(
            int page,
            int size,
            long total,

            @JsonProperty("total_pages")
            int totalPages,

            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
