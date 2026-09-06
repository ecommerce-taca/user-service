package com.ecommerce.authuser.shopfollow.application.count;

import java.util.UUID;

public record GetFollowerCountResult(
        UUID shopId,
        long followerCount
) {
}
