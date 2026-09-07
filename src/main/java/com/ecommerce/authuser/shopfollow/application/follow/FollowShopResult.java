package com.ecommerce.authuser.shopfollow.application.follow;

import java.time.Instant;
import java.util.UUID;

public record FollowShopResult(
        UUID shopId,
        Instant followedAt,
        boolean created
) {
}
