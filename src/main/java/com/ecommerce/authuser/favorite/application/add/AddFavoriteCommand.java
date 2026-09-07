package com.ecommerce.authuser.favorite.application.add;

import java.util.UUID;

public record AddFavoriteCommand(
        UUID userId,
        UUID productId
) {
}
