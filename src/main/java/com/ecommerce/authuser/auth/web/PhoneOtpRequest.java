package com.ecommerce.authuser.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneOtpRequest(

        @NotBlank(message = "phone is required")
        @Pattern(
                regexp = "^\\+[1-9]\\d{7,14}$",
                message = "phone must be E.164"
        )
        String phone
) {
}
