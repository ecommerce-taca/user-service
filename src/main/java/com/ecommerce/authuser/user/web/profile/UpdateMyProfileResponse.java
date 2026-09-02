package com.ecommerce.authuser.user.web.profile;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateMyProfileResponse(
        Data data,
        Meta meta
) {

    public record Data(
            UUID id,

            @JsonProperty("full_name")
            String fullName,

            String email,

            @JsonProperty("email_verified")
            boolean emailVerified,

            String phone,

            @JsonProperty("phone_verified")
            boolean phoneVerified,

            @JsonProperty("date_of_birth")
            LocalDate dateOfBirth,

            List<String> roles,

            String status,

            @JsonProperty("default_shop_id")
            UUID defaultShopId,

            @JsonProperty("created_at")
            Instant createdAt,

            @JsonProperty("updated_at")
            Instant updatedAt,

            @JsonProperty("phone_verification_required")
            boolean phoneVerificationRequired
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
