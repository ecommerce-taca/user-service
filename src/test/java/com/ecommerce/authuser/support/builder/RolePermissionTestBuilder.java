package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.rbac.domain.Permission;
import com.ecommerce.authuser.rbac.domain.Role;
import com.ecommerce.authuser.rbac.domain.RolePermission;

import java.util.UUID;

public final class RolePermissionTestBuilder {

    private Role role;
    private Permission permission;
    private UUID grantedBy;

    private RolePermissionTestBuilder() {}

    public static RolePermissionTestBuilder aRolePermission() {
        return new RolePermissionTestBuilder();
    }

    public static RolePermissionTestBuilder grant(
            Role role,
            Permission permission
    ) {
        return new RolePermissionTestBuilder()
                .withRole(role)
                .withPermission(permission);
    }

    public RolePermissionTestBuilder withRole(Role role) {
        this.role = role;
        return this;
    }

    public RolePermissionTestBuilder withPermission(Permission permission) {
        this.permission = permission;
        return this;
    }

    public RolePermissionTestBuilder withGrantedBy(UUID grantedBy) {
        this.grantedBy = grantedBy;
        return this;
    }

    public RolePermission build() {
        if (role == null) {
            throw new IllegalStateException(
                    "Role must not be null"
            );
        }

        if (permission == null) {
            throw new IllegalStateException(
                    "Permission must not be null"
            );
        }

        return RolePermission.grant(
                role,
                permission,
                grantedBy
        );
    }
}
