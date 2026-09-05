package com.ecommerce.authuser.user.application.admin.status;

import com.ecommerce.authuser.user.domain.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminUserStatusResult(
        UUID userId,
        UserStatus oldStatus,
        UserStatus newStatus,
        Instant changedAt,
        int revokedRefreshTokens
) {
}
