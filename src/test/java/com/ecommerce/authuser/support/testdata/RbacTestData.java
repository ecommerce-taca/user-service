package com.ecommerce.authuser.support.testdata;

import com.ecommerce.authuser.rbac.domain.Permission;
import com.ecommerce.authuser.rbac.domain.Role;
import com.ecommerce.authuser.rbac.domain.RolePermission;
import com.ecommerce.authuser.support.builder.PermissionTestBuilder;
import com.ecommerce.authuser.support.builder.RolePermissionTestBuilder;
import com.ecommerce.authuser.support.builder.RoleTestBuilder;

public final class RbacTestData {
    private RbacTestData() {
    }

    public static Role buyerRole() {
        return RoleTestBuilder.buyer()
                .build();
    }

    public static Role userRole(String roleKey) {
        return RoleTestBuilder.userRole(roleKey)
                .build();
    }

    public static Role systemRole(String roleKey) {
        return RoleTestBuilder.aRole()
                .withRoleKey(roleKey)
                .asSystemRole()
                .build();
    }

    public static Permission userReadPermission() {
        return PermissionTestBuilder.userRead()
                .build();
    }

    public static Permission userUpdatePermission() {
        return PermissionTestBuilder.userUpdate()
                .build();
    }

    public static Permission userCreatePermission() {
        return PermissionTestBuilder.userCreate()
                .build();
    }

    public static Permission permission(String permissionKey) {
        return PermissionTestBuilder.aPermission()
                .withPermissionKey(permissionKey)
                .build();
    }

    public static RolePermission grant(
            Role role,
            Permission permission) {
        return RolePermissionTestBuilder.grant(
                role,
                permission).build();
    }

    public static RolePermission grant(
            Role role,
            Permission permission,
            java.util.UUID grantedBy) {
        return RolePermissionTestBuilder.grant(
                role,
                permission)
                .withGrantedBy(grantedBy)
                .build();
    }
}
