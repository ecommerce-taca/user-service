package com.ecommerce.authuser.shop.application.publicprofile;

import com.ecommerce.authuser.shop.domain.ShopStatus;

import java.time.Instant;
import java.util.UUID;

public record GetPublicShopResult(
        UUID shopId,
        String name,
        String slug,
        String logoObjectKey,
        String description,
        boolean verified,
        ShopStatus status,
        long followerCount,
        Instant createdAt
) {
}
