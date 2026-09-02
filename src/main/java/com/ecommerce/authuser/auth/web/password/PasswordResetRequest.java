package com.ecommerce.authuser.auth.web.password;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(

        @NotBlank(message = "token is required")
        @Size(max = 512, message = "token is too long")
        String token,

        @JsonProperty("new_password")
        @NotBlank(message = "new_password is required")
        @Size(min = 12, max = 72, message = "new_password must be between 12 and 72 characters")
        String newPassword

) {
}