package com.ecommerce.authuser.auth.web.session;

import com.ecommerce.authuser.auth.web.common.AuthTokenData;
import com.ecommerce.authuser.common.web.RequestMeta;
import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshResponse(
        Data data,
        RequestMeta meta
) {

    public record Data(
            AuthTokenData tokens
    ) {
    }
}