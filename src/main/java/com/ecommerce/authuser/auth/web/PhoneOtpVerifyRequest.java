package com.ecommerce.authuser.auth.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record PhoneOtpVerifyRequest(

        @JsonProperty("challenge_id")
        @NotNull(message = "challenge_id is required")
        UUID challengeId,

        @NotBlank(message = "otp is required")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "otp must contain exactly 6 digits"
        )
        String otp
) {
}
