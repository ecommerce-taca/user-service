package com.ecommerce.authuser.user.application.admin.status;

import java.util.UUID;

public record AdminUserStatusCommand(
        UUID actorUserId,
        UUID sessionId,
        UUID targetUserId,
        String status,
        String reason,
        String stepUpToken,
        String clientIp
) {
}
