package com.ecommerce.authuser.shop.web.publicprofile;

import com.ecommerce.authuser.shop.domain.ShopStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record GetPublicShopResponse(
        Data data,
        Meta meta
) {

    public record Data(
            UUID id,

            String name,

            String slug,

            @JsonProperty("logo_url")
            String logoUrl,

            String description,

            @JsonProperty("is_verified")
            boolean verified,

            ShopStatus status,

            @JsonProperty("follower_count")
            long followerCount,

            @JsonProperty("created_at")
            Instant createdAt
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
