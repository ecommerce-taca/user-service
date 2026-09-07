package com.ecommerce.authuser.favorite.application.add;

import java.time.Instant;
import java.util.UUID;

public record AddFavoriteResult(
        UUID productId,
        Instant createdAt,
        boolean created
) {
}
