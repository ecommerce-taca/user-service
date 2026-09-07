package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.shop.domain.ShopStaff;
import com.ecommerce.authuser.user.domain.User;

public final class ShopStaffTestBuilder {

    private Shop shop;

    private User user;

    private String staffRole = "STAFF";

    private User invitedBy;

    private ShopStaffTestBuilder() {
    }

    public static ShopStaffTestBuilder aShopStaff() {
        return new ShopStaffTestBuilder();
    }

    public static ShopStaffTestBuilder invite(
            Shop shop,
            User user,
            User invitedBy
    ) {
        return new ShopStaffTestBuilder()
                .withShop(shop)
                .withUser(user)
                .withInvitedBy(invitedBy);
    }

    public static ShopStaffTestBuilder manager(
            Shop shop,
            User user,
            User invitedBy
    ) {
        return new ShopStaffTestBuilder()
                .withShop(shop)
                .withUser(user)
                .withStaffRole("MANAGER")
                .withInvitedBy(invitedBy);
    }

    public static ShopStaffTestBuilder staff(
            Shop shop,
            User user,
            User invitedBy
    ) {
        return new ShopStaffTestBuilder()
                .withShop(shop)
                .withUser(user)
                .withStaffRole("STAFF")
                .withInvitedBy(invitedBy);
    }

    public ShopStaffTestBuilder withShop(
            Shop shop
    ) {
        this.shop = shop;
        return this;
    }

    public ShopStaffTestBuilder withUser(
            User user
    ) {
        this.user = user;
        return this;
    }

    public ShopStaffTestBuilder withStaffRole(
            String staffRole
    ) {
        this.staffRole = staffRole;
        return this;
    }

    public ShopStaffTestBuilder withInvitedBy(
            User invitedBy
    ) {
        this.invitedBy = invitedBy;
        return this;
    }

    public ShopStaff build() {
        if (shop == null) {
            throw new IllegalStateException(
                    "Shop must not be null"
            );
        }

        if (user == null) {
            throw new IllegalStateException(
                    "User must not be null"
            );
        }

        if (invitedBy == null) {
            throw new IllegalStateException(
                    "InvitedBy must not be null"
            );
        }

        if (staffRole == null || staffRole.isBlank()) {
            throw new IllegalStateException(
                    "Staff role must not be blank"
            );
        }

        return ShopStaff.invite(
                shop,
                user,
                staffRole,
                invitedBy
        );
    }
}
