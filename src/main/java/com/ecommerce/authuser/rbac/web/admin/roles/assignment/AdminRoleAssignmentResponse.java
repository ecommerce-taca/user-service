package com.ecommerce.authuser.rbac.web.admin.roles.assignment;

import com.ecommerce.authuser.rbac.application.admin.roles.assignment.AdminRoleAssignmentAction;
import com.ecommerce.authuser.rbac.domain.ScopeType;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record AdminRoleAssignmentResponse(
        Data data,
        Meta meta
) {

    public record Data(
            @JsonProperty("user_id")
            UUID userId,

            String role,

            @JsonProperty("scope_type")
            ScopeType scopeType,

            @JsonProperty("shop_id")
            UUID shopId,

            AdminRoleAssignmentAction action,

            @JsonProperty("changed_at")
            Instant changedAt
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}