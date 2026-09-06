package com.ecommerce.authuser.common.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RequestMeta(
        @JsonProperty("request_id")
        String requestId
) {
}