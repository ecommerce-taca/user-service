package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.rbac.domain.Permission;
import com.ecommerce.authuser.rbac.domain.ScopeType;

public final class PermissionTestBuilder {

    private String permissionKey = "user:read";
    private ScopeType scopeType = ScopeType.USER;
    private String description = "Read user information";

    private PermissionTestBuilder() {}

    public static PermissionTestBuilder aPermission() {
        return new PermissionTestBuilder();
    }

    public static PermissionTestBuilder userRead() {
        return new PermissionTestBuilder()
                .withPermissionKey("user:read")
                .withScopeType(ScopeType.USER)
                .withDescription("Read user information");
    }

    public static PermissionTestBuilder userUpdate() {
        return new PermissionTestBuilder()
                .withPermissionKey("user:update")
                .withScopeType(ScopeType.USER)
                .withDescription("Update user information");
    }

    public static PermissionTestBuilder userCreate() {
        return new PermissionTestBuilder()
                .withPermissionKey("user:create")
                .withScopeType(ScopeType.USER)
                .withDescription("Create user");
    }

    public PermissionTestBuilder withPermissionKey(String permissionKey) {
        this.permissionKey = permissionKey;
        return this;
    }

    public PermissionTestBuilder withScopeType(ScopeType scopeType) {
        this.scopeType = scopeType;
        return this;
    }

    public PermissionTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public Permission build() {
        return Permission.create(
                permissionKey,
                scopeType,
                description
        );
    }
}