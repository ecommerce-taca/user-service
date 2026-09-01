package com.ecommerce.authuser.auth.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshResponse(
        Data data,
        Meta meta
) {

    public record Data(
            TokenData tokens
    ) {
    }

    public record TokenData(

            @JsonProperty("token_type")
            String tokenType,

            @JsonProperty("access_token")
            String accessToken,

            @JsonProperty("expires_in")
            long expiresIn,

            @JsonProperty("refresh_token")
            String refreshToken,

            @JsonProperty("refresh_expires_in")
            long refreshExpiresIn
    ) {
    }

    public record Meta(

            @JsonProperty("request_id")
            String requestId
    ) {
    }
}