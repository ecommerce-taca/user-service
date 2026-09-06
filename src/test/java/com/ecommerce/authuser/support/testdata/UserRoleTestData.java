package com.ecommerce.authuser.support.testdata;

import com.ecommerce.authuser.rbac.domain.Role;
import com.ecommerce.authuser.rbac.domain.UserRole;
import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.support.builder.UserRoleTestBuilder;
import com.ecommerce.authuser.user.domain.User;

public final class UserRoleTestData {

    private UserRoleTestData() {
    }

    public static UserRole buyer(
            User user,
            Role buyerRole
    ) {
        return UserRoleTestBuilder
                .assign(
                        user,
                        buyerRole
                )
                .build();
    }

    public static UserRole shopRole(
            User user,
            Role role,
            Shop shop
    ) {
        return UserRoleTestBuilder
                .assignShopRole(
                        user,
                        role,
                        shop
                )
                .build();
    }
}
