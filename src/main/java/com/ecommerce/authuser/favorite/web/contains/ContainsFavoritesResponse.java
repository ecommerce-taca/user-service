package com.ecommerce.authuser.favorite.web.contains;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record ContainsFavoritesResponse(
        Map<String, Boolean> data,
        Meta meta
) {

    public record Meta(

            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
