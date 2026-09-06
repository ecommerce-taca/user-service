package com.ecommerce.authuser.shopfollow.web.count;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GetFollowerCountResponse(
        Data data,
        Meta meta
) {

    public record Data(
            @JsonProperty("shop_id")
            String shopId,

            @JsonProperty("follower_count")
            long followerCount
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
