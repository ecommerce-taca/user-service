package com.ecommerce.authuser.auth.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @JsonProperty("full_name")
        @NotBlank
        @Size(max = 120)
        String fullName,

        @NotBlank
        @Size(min = 3, max = 254)
        @Email
        String email,

        @NotBlank
        @Size(min = 12, max = 72)
        String password,

        @Pattern(
                regexp = "^\\+[1-9][0-9]{7,14}$",
                message = "phone must be in E.164 format"
        )
        String phone

) {
}