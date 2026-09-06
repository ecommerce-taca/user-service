package com.ecommerce.authuser.favorite.application.delete;

import java.util.UUID;

public record DeleteFavoriteCommand(
        UUID userId,
        UUID productId
) {
}
