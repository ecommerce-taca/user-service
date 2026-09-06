package com.ecommerce.authuser.shopfollow.web.follow;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record FollowShopResponse(
        Data data,
        Meta meta
) {

    public record Data(
            @JsonProperty("shop_id")
            UUID shopId,

            @JsonProperty("followed_at")
            Instant followedAt
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
