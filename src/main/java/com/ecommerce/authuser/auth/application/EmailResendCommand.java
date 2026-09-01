package com.ecommerce.authuser.auth.application;
import java.util.UUID;

public record EmailResendCommand(
        UUID userId,
        String clientIp
) {
}