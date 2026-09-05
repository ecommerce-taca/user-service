package com.ecommerce.authuser.rbac.web.admin.roles.assignment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminRoleAssignmentRequest(
        String action,
        String role,

        @JsonProperty("shop_id")
        String shopId,

        String reason
) {
}