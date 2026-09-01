package com.ecommerce.authuser.auth.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SignupResponse(
        Data data,
        Meta meta
) {

    public record Data(
            UserData user,
            TokenData tokens,
            VerificationData verification
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

    public record TokenData(
            @JsonProperty("token_type") String tokenType,

            @JsonProperty("access_token") String accessToken,

            @JsonProperty("expires_in") long expiresIn,

            @JsonProperty("refresh_token") String refreshToken,

            @JsonProperty("refresh_expires_in") long refreshExpiresIn
    ) {
    }

    public record VerificationData(
            @JsonProperty("email_sent") boolean emailSent,

            @JsonProperty("expires_at") Instant expiresAt
    ) {
    }

    public record Meta(
            @JsonProperty("request_id") String requestId
    ) {
    }
}
