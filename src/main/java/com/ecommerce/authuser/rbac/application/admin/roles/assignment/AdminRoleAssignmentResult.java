package com.ecommerce.authuser.rbac.application.admin.roles.assignment;

import com.ecommerce.authuser.rbac.domain.ScopeType;

import java.time.Instant;
import java.util.UUID;

public record AdminRoleAssignmentResult(
        UUID userId,
        String role,
        ScopeType scopeType,
        UUID shopId,
        AdminRoleAssignmentAction action,
        Instant changedAt
) {
}