package com.ecommerce.authuser.auth.web.password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordForgotRequest(

        @NotBlank(message = "identifier is required")
        @Size(max = 254, message = "identifier is too long")
        String identifier
) {
}
