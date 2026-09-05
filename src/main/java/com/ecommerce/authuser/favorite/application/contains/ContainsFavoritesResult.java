package com.ecommerce.authuser.favorite.application.contains;

import java.util.Map;
import java.util.UUID;

public record ContainsFavoritesResult(
        Map<UUID, Boolean> statuses
) {
}
