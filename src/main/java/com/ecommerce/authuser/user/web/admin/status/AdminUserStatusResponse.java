package com.ecommerce.authuser.user.web.admin.status;

import com.ecommerce.authuser.user.domain.UserStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record AdminUserStatusResponse(
        Data data,
        Meta meta
) {

    public record Data(
            @JsonProperty("user_id")
            UUID userId,

            UserStatus status,

            @JsonProperty("changed_at")
            Instant changedAt
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
