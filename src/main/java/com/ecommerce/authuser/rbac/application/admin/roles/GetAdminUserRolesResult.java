package com.ecommerce.authuser.rbac.application.admin.roles;

import com.ecommerce.authuser.rbac.domain.ScopeType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GetAdminUserRolesResult(
        UUID userId,
        List<AssignmentResult> assignments
) {

    public record AssignmentResult(
            String role,
            ScopeType scopeType,
            UUID shopId,
            List<String> permissions,
            Instant grantedAt,
            UUID grantedBy
    ) {
    }
}
