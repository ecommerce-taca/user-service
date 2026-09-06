package com.ecommerce.authuser.shopfollow.application.follow;

import java.util.UUID;

public record FollowShopCommand(
        UUID userId,
        UUID shopId
) {
}
