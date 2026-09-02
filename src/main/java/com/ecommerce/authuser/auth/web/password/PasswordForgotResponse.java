package com.ecommerce.authuser.auth.web.password;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PasswordForgotResponse(
        Data data,
        Meta meta
) {

    public record Data(
            boolean accepted,
            String message
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
