package com.ecommerce.authuser.shopfollow.application.unfollow;

import java.util.UUID;

public record UnfollowShopCommand(
        UUID userId,
        UUID shopId
) {
}
