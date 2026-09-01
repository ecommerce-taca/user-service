package com.ecommerce.authuser.common.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApiErrorResponse(ErrorData error) {

    public record ErrorData(
            String code,
            String message,
            Object details,

            @JsonProperty("trace_id")
            String traceId
    ) {
    }
}
