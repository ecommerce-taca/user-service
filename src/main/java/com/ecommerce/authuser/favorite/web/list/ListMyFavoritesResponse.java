package com.ecommerce.authuser.favorite.web.list;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ListMyFavoritesResponse(
        List<Data> data,
        Meta meta
) {

    public record Data(

            @JsonProperty("product_id")
            UUID productId,

            @JsonProperty("created_at")
            Instant createdAt
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
