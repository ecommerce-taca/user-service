package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.rbac.domain.Role;
import com.ecommerce.authuser.rbac.domain.UserRole;
import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.user.domain.User;

import java.util.UUID;

public final class UserRoleTestBuilder {

    private User user;
    private Role role;
    private Shop shop;
    private UUID grantedBy;

    private UserRoleTestBuilder() {
    }

    public static UserRoleTestBuilder aUserRole() {
        return new UserRoleTestBuilder();
    }

    public static UserRoleTestBuilder assign(
            User user,
            Role role
    ) {
        return new UserRoleTestBuilder()
                .withUser(user)
                .withRole(role);
    }

    public static UserRoleTestBuilder assignShopRole(
            User user,
            Role role,
            Shop shop
    ) {
        return new UserRoleTestBuilder()
                .withUser(user)
                .withRole(role)
                .withShop(shop);
    }

    public UserRoleTestBuilder withUser(
            User user
    ) {
        this.user = user;
        return this;
    }

    public UserRoleTestBuilder withRole(
            Role role
    ) {
        this.role = role;
        return this;
    }

    public UserRoleTestBuilder withShop(
            Shop shop
    ) {
        this.shop = shop;
        return this;
    }

    public UserRoleTestBuilder withGrantedBy(
            UUID grantedBy
    ) {
        this.grantedBy = grantedBy;
        return this;
    }

    public UserRole build() {
        if (user == null) {
            throw new IllegalStateException(
                    "User must not be null"
            );
        }

        if (role == null) {
            throw new IllegalStateException(
                    "Role must not be null"
            );
        }

        return UserRole.assign(
                user,
                role,
                shop,
                grantedBy
        );
    }
}
