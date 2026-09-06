package com.ecommerce.authuser.rbac.application.admin.roles.assignment;

import java.util.UUID;

public record AdminRoleAssignmentCommand(
        UUID actorUserId,
        UUID sessionId,
        UUID targetUserId,
        String action,
        String role,
        UUID shopId,
        String reason,
        String stepUpToken,
        String clientIp
) {
}
