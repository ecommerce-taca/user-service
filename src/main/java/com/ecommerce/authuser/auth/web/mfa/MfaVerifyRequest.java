package com.ecommerce.authuser.auth.web.mfa;

import com.ecommerce.authuser.mfa.domain.MfaMethod;
import com.ecommerce.authuser.mfa.domain.MfaPurpose;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MfaVerifyRequest(

        @NotNull(message = "purpose is required")
        MfaPurpose purpose,

        @JsonProperty("challenge_id")
        UUID challengeId,

        @JsonProperty("setup_id")
        UUID setupId,

        @NotNull(message = "method is required")
        MfaMethod method,

        @NotBlank(message = "code is required")
        @Size(max = 128, message = "code is too long")
        String code

) {
}
