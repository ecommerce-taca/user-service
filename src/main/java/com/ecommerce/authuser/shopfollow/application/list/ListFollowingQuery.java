package com.ecommerce.authuser.shopfollow.application.list;

import java.util.UUID;

public record ListFollowingQuery(
        UUID userId,
        Integer page,
        Integer size,
        String sort
) {
}
