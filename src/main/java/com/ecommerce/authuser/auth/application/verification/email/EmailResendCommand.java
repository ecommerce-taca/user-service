package com.ecommerce.authuser.auth.application.verification.email;
import java.util.UUID;

public record EmailResendCommand(
        UUID userId,
        String clientIp
) {
}