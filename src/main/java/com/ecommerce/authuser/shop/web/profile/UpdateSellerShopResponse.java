package com.ecommerce.authuser.shop.web.profile;

import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.ShopStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record UpdateSellerShopResponse(
        Data data,
        Meta meta
) {

    public record Data(
            UUID id,

            String name,

            String slug,

            @JsonProperty("business_name")
            String businessName,

            String description,

            @JsonProperty("logo_url")
            String logoUrl,

            ShopStatus status,

            @JsonProperty("kyc_status")
            KycStatus kycStatus,

            @JsonProperty("updated_at")
            Instant updatedAt
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
