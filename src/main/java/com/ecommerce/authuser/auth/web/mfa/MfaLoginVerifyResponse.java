package com.ecommerce.authuser.auth.web.mfa;

import com.ecommerce.authuser.auth.web.common.AuthTokenData;
import com.ecommerce.authuser.common.web.RequestMeta;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MfaLoginVerifyResponse(
        Data data,
        RequestMeta meta
) {

    public record Data(
            AuthTokenData tokens
    ) {
    }
}
