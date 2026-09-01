package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.user.domain.User;

public final class ShopTestBuilder {

    private User owner;
    private String name = "Test Shop";
    private String slug = "test-shop";
    private String businessName = "Test Shop Business";
    private String taxCode;
    private String description = "Test shop description";

    private ShopTestBuilder() {
    }

    public static ShopTestBuilder aShop() {
        return new ShopTestBuilder();
    }

    public static ShopTestBuilder forOwner(
            User owner
    ) {
        return new ShopTestBuilder()
                .withOwner(owner);
    }

    public static ShopTestBuilder defaultShop(
            User owner
    ) {
        return new ShopTestBuilder()
                .withOwner(owner)
                .withName("Test Shop")
                .withSlug("test-shop")
                .withBusinessName("Test Shop Business")
                .withDescription("Test shop description");
    }

    public ShopTestBuilder withOwner(
            User owner
    ) {
        this.owner = owner;
        return this;
    }

    public ShopTestBuilder withName(
            String name
    ) {
        this.name = name;
        return this;
    }

    public ShopTestBuilder withSlug(
            String slug
    ) {
        this.slug = slug;
        return this;
    }

    public ShopTestBuilder withBusinessName(
            String businessName
    ) {
        this.businessName = businessName;
        return this;
    }

    public ShopTestBuilder withTaxCode(
            String taxCode
    ) {
        this.taxCode = taxCode;
        return this;
    }

    public ShopTestBuilder withDescription(
            String description
    ) {
        this.description = description;
        return this;
    }

    public Shop build() {
        if (owner == null) {
            throw new IllegalStateException(
                    "Owner must not be null"
            );
        }

        return Shop.create(
                owner,
                name,
                slug,
                businessName,
                taxCode,
                description
        );
    }
}
