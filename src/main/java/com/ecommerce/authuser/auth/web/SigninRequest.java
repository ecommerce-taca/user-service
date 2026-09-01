package com.ecommerce.authuser.auth.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SigninRequest(

        @NotBlank
        @Size(max = 254)
        String identifier,

        @NotBlank
        @Size(max = 72)
        String password,

        @JsonProperty("remember_me")
        Boolean rememberMe
) {
    public boolean resolvedRememberMe() {
        return rememberMe == null || rememberMe;
    }
}
