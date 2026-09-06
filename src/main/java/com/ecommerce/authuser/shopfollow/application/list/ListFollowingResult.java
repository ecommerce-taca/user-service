package com.ecommerce.authuser.shopfollow.application.list;

import java.util.List;

public record ListFollowingResult(
        List<FollowingShopResult> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
