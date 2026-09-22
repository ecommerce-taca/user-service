package com.ecommerce.authuser.location.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WardListResponse(
        List<Data> data,
        Meta meta
) {

    public record Data(
            String code,
            String name,

            @JsonProperty("province_code")
            String provinceCode
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
