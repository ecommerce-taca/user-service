package com.ecommerce.authuser.rbac.web.admin.roles;

import com.ecommerce.authuser.rbac.domain.ScopeType;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserRolesResponse(
        Data data,
        Meta meta
) {

    public record Data(
            @JsonProperty("user_id")
            UUID userId,

            List<Assignment> assignments
    ) {
    }

    public record Assignment(
            String role,

            @JsonProperty("scope_type")
            ScopeType scopeType,

            @JsonProperty("shop_id")
            UUID shopId,

            List<String> permissions,

            @JsonProperty("granted_at")
            Instant grantedAt,

            @JsonProperty("granted_by")
            UUID grantedBy
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
