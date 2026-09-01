package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.shop.domain.SellerOnboarding;
import com.ecommerce.authuser.shop.domain.Shop;

public final class SellerOnboardingTestBuilder {

    private Shop shop;

    private SellerOnboardingTestBuilder() {
    }

    public static SellerOnboardingTestBuilder anOnboarding() {
        return new SellerOnboardingTestBuilder();
    }

    public static SellerOnboardingTestBuilder forShop(
            Shop shop
    ) {
        return new SellerOnboardingTestBuilder()
                .withShop(shop);
    }

    public SellerOnboardingTestBuilder withShop(
            Shop shop
    ) {
        this.shop = shop;
        return this;
    }

    public SellerOnboarding build() {
        if (shop == null) {
            throw new IllegalStateException(
                    "Shop must not be null"
            );
        }

        return SellerOnboarding.create(shop);
    }
}
