package com.ecommerce.authuser.favorite.application.list;

import java.util.List;

public record ListMyFavoritesResult(
        List<FavoriteResult> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}