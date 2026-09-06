package com.ecommerce.authuser.shopfollow.web.list;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ListFollowingResponse(
        List<Data> data,
        Meta meta
) {

    public record Data(

            @JsonProperty("shop_id")
            UUID shopId,

            String name,

            String slug,

            @JsonProperty("logo_url")
            String logoUrl,

            @JsonProperty("followed_at")
            Instant followedAt
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
