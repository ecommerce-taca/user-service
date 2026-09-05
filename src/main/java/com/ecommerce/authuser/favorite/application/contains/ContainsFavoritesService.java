package com.ecommerce.authuser.favorite.application.contains;

import com.ecommerce.authuser.favorite.exception.InvalidFavoriteQueryException;
import com.ecommerce.authuser.favorite.repository.UserFavoriteRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContainsFavoritesService {

    private static final int MAX_PRODUCT_IDS = 100;

    private final UserFavoriteRepository userFavoriteRepository;

    @Transactional(readOnly = true)
    public ContainsFavoritesResult contains(ContainsFavoritesQuery query) {

        if (query == null
                || query.userId() == null
                || query.productIds() == null
                || query.productIds().isEmpty()
                || query.productIds().size() > MAX_PRODUCT_IDS
                || query.productIds().stream().anyMatch(id -> id == null)) {

            throw new InvalidFavoriteQueryException();
        }

        List<UUID> requestedIds =
                List.copyOf(
                        new LinkedHashSet<>(
                                query.productIds()
                        )
                );

        List<UUID> existingIds = userFavoriteRepository
                .findExistingProductIds(
                        query.userId(),
                        requestedIds
                );

        Set<UUID> existingSet = Set.copyOf(existingIds);

        Map<UUID, Boolean> statuses = new LinkedHashMap<>();

        for (UUID productId : requestedIds) {

            statuses.put(
                    productId,
                    existingSet.contains(productId)
            );
        }

        return new ContainsFavoritesResult(
                Map.copyOf(statuses)
        );
    }
}
