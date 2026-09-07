package com.ecommerce.authuser.auth.application.verification.email;

public record EmailVerificationCommand(
        String token
) {
}
