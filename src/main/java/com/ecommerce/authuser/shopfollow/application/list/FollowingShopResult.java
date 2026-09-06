package com.ecommerce.authuser.shopfollow.application.list;

import java.time.Instant;
import java.util.UUID;

public record FollowingShopResult(
        UUID shopId,
        String name,
        String slug,
        String logoObjectKey,
        Instant followedAt
) {
}
