package com.ecommerce.authuser.auth.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

public record SignoutRequest(

        @JsonProperty("refresh_token")
        @Size(min = 43, max = 512)
        String refreshToken,

        @JsonProperty("all_sessions")
        Boolean allSessions
) {

    public boolean resolvedAllSessions() {
        return Boolean.TRUE.equals(allSessions);
    }
}
