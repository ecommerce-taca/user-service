package com.ecommerce.authuser.auth.web.password;

import com.ecommerce.authuser.common.web.RequestMeta;
import com.fasterxml.jackson.annotation.JsonProperty;

public record PasswordForgotResponse(
        Data data,
        RequestMeta meta
) {

    public record Data(
            boolean accepted,
            String message
    ) {
    }
}
