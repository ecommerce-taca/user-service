package com.ecommerce.authuser.rbac.application.admin.roles;

import java.util.UUID;

public record GetAdminUserRolesQuery(
        UUID actorUserId,
        UUID targetUserId
) {
}
