package com.ecommerce.authuser.auth.application;

public record EmailVerificationCommand(
        String token
) {
}
