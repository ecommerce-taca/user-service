package com.ecommerce.authuser.favorite.application.list;

import java.time.Instant;
import java.util.UUID;

public record FavoriteResult(
        UUID productId,
        Instant createdAt
) {
}
