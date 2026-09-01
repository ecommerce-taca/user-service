package com.ecommerce.authuser.auth.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(

        @JsonProperty("refresh_token")
        @NotBlank
        @Size(min = 43, max = 512)
        String refreshToken
) {
}
