package com.ecommerce.authuser.favorite.application.contains;

import java.util.List;
import java.util.UUID;

public record ContainsFavoritesQuery(
        UUID userId,
        List<UUID> productIds
) {
}
