package com.ecommerce.authuser.auth.web.signin;

import com.ecommerce.authuser.auth.web.common.AuthTokenData;
import com.ecommerce.authuser.common.web.RequestMeta;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record SigninResponse(
        Data data,
        RequestMeta meta
) {

    public record Data(
            UserData user,
            AuthTokenData tokens
    ) {
    }

    public record UserData(
            UUID id,

            @JsonProperty("full_name")
            String fullName,

            String email,

            @JsonProperty("email_verified")
            boolean emailVerified,

            String phone,

            @JsonProperty("phone_verified")
            boolean phoneVerified,

            List<String> roles,

            String status
    ) {
    }
}