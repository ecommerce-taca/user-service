package com.ecommerce.authuser.favorite.application.list;

import java.util.UUID;

public record ListMyFavoritesQuery(
        UUID userId,
        Integer page,
        Integer size,
        String sort
) {
}
