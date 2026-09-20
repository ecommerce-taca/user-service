package com.ecommerce.authuser.location.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProvinceListResponse(
        List<Data> data,
        Meta meta
) {

    public record Data(
            String code,
            String name,

            @JsonProperty("short_name")
            String shortName,

            @JsonProperty("place_type")
            String placeType
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
