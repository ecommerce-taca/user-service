package com.ecommerce.authuser.auth.web.verification.email;

import jakarta.validation.constraints.Pattern;

public record EmailResendRequest(

        @Pattern(
                regexp = "EMAIL_VERIFY",
                message = "purpose must be EMAIL_VERIFY"
        )
        String purpose
) {
}