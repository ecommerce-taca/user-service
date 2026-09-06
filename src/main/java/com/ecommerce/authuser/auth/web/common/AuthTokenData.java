package com.ecommerce.authuser.auth.web.common;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthTokenData(

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
