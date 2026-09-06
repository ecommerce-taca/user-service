package com.ecommerce.authuser.support.fixture;

import com.ecommerce.authuser.rbac.domain.Role;
import com.ecommerce.authuser.rbac.domain.UserRole;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;
import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.support.builder.UserRoleTestBuilder;
import com.ecommerce.authuser.user.domain.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class UserRoleFixture {

    private final UserRoleRepository userRoleRepository;

    public UserRoleFixture(
            UserRoleRepository userRoleRepository
    ) {
        this.userRoleRepository = userRoleRepository;
    }

    public UserRole assign(
            User user,
            Role role
    ) {
        return assign(
                user,
                role,
                null,
                null
        );
    }

    public UserRole assign(
            User user,
            Role role,
            UUID grantedBy
    ) {
        return assign(
                user,
                role,
                null,
                grantedBy
        );
    }

    public UserRole assignShopRole(
            User user,
            Role role,
            Shop shop
    ) {
        return assign(
                user,
                role,
                shop,
                null
        );
    }

    public UserRole assignShopRole(
            User user,
            Role role,
            Shop shop,
            UUID grantedBy
    ) {
        return assign(
                user,
                role,
                shop,
                grantedBy
        );
    }

    private UserRole assign(
            User user,
            Role role,
            Shop shop,
            UUID grantedBy
    ) {
        UserRole userRole = UserRoleTestBuilder
                .aUserRole()
                .withUser(user)
                .withRole(role)
                .withShop(shop)
                .withGrantedBy(grantedBy)
                .build();

        return userRoleRepository.save(userRole);
    }
}
