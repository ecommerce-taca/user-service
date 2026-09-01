package com.ecommerce.authuser.auth.application;

import java.util.UUID;

public record SignoutCommand(
        UUID userId,
        UUID sessionId,
        String refreshToken,
        boolean allSessions,
        String clientIp
) {
}
