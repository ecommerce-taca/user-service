package com.ecommerce.authuser.favorite.web.add;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record AddFavoriteResponse(
        Data data,
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

            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
