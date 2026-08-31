package com.ecommerce.authuser.support.testdata;

import com.ecommerce.authuser.rbac.domain.Permission;
import com.ecommerce.authuser.rbac.domain.Role;
import com.ecommerce.authuser.support.builder.PermissionTestBuilder;
import com.ecommerce.authuser.support.builder.RoleTestBuilder;

public final class RbacTestData {

    public static final String BUYER_ROLE_KEY = "BUYER";
    public static final String USER_READ_PERMISSION_KEY = "user:read";
    public static final String USER_UPDATE_PERMISSION_KEY = "user:update";
    public static final String USER_CREATE_PERMISSION_KEY = "user:create";

    private RbacTestData() {}

    public static RoleTestBuilder buyerRole() {
        return RoleTestBuilder.buyer();
    }

    public static RoleTestBuilder userRole(
            String roleKey
    ) {
        return RoleTestBuilder.userRole(roleKey);
    }

    public static PermissionTestBuilder userReadPermission() {
        return PermissionTestBuilder.userRead();
    }

    public static PermissionTestBuilder userUpdatePermission() {
        return PermissionTestBuilder.userUpdate();
    }

    public static PermissionTestBuilder userCreatePermission() {
        return PermissionTestBuilder.userCreate();
    }
}
