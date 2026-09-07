package com.ecommerce.authuser.support.testdata;

import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.support.builder.ShopTestBuilder;
import com.ecommerce.authuser.user.domain.User;
public final class ShopTestData {
private ShopTestData() {
}

public static Shop defaultShop(
        User owner
) {
    return ShopTestBuilder.aShop()
            .withOwner(owner)
            .withName("Test Shop")
            .withSlug("test-shop")
            .withBusinessName("Test Shop Business")
            .withTaxCode("0123456789")
            .withDescription("Test shop")
            .build();
}

public static Shop draftShop(
        User owner
) {
    return ShopTestBuilder.aShop()
            .withOwner(owner)
            .withName("Draft Shop")
            .withSlug("draft-shop")
            .withBusinessName("Draft Shop Business")
            .withTaxCode("0123456790")
            .withDescription("Draft shop")
            .build();
}

public static Shop shop(
        User owner,
        String name,
        String slug,
        String businessName
) {
    return ShopTestBuilder.aShop()
            .withOwner(owner)
            .withName(name)
            .withSlug(slug)
            .withBusinessName(businessName)
            .build();
}

public static Shop shopWithTaxCode(
        User owner,
        String name,
        String slug,
        String businessName,
        String taxCode
) {
    return ShopTestBuilder.aShop()
            .withOwner(owner)
            .withName(name)
            .withSlug(slug)
            .withBusinessName(businessName)
            .withTaxCode(taxCode)
            .build();
}
}


