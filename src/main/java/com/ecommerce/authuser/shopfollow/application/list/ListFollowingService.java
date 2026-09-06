package com.ecommerce.authuser.shopfollow.application.list;

import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.shop.repository.ShopRepository;

import com.ecommerce.authuser.shopfollow.domain.ShopFollow;
import com.ecommerce.authuser.shopfollow.exception.InvalidShopFollowQueryException;
import com.ecommerce.authuser.shopfollow.repository.ShopFollowRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListFollowingService {

    private static final int DEFAULT_PAGE = 1;

    private static final int DEFAULT_SIZE = 20;

    private static final int MAX_SIZE = 100;

    private static final String DEFAULT_SORT = "created_at,desc";

    private final ShopFollowRepository shopFollowRepository;

    private final ShopRepository shopRepository;

    @Transactional(readOnly = true)
    public ListFollowingResult list(ListFollowingQuery query) {

        Objects.requireNonNull(
                query,
                "query must not be null"
        );

        Objects.requireNonNull(
                query.userId(),
                "userId must not be null"
        );

        int page = resolvePage(query.page());

        int size = resolveSize(query.size());

        Sort sort = resolveSort(query.sort());

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<ShopFollow> followPage = shopFollowRepository
                .findAllById_UserId(
                        query.userId(),
                        pageable
                );

        List<UUID> shopIds =
                followPage
                        .getContent()
                        .stream()
                        .map(ShopFollow::getShopId)
                        .toList();

        Map<UUID, Shop> shopsById = loadShopsById(shopIds);

        List<FollowingShopResult> items =
                followPage
                        .getContent()
                        .stream()
                        .map(
                                follow ->
                                        toResult(
                                                follow,
                                                shopsById
                                        )
                        )
                        .toList();

        return new ListFollowingResult(
                items,
                page,
                size,
                followPage.getTotalElements(),
                followPage.getTotalPages()
        );
    }

    private Map<UUID, Shop> loadShopsById(List<UUID> shopIds) {

        if (shopIds.isEmpty()) {
            return Map.of();
        }

        List<Shop> shops = shopRepository.findAllById(shopIds);

        Map<UUID, Shop> result =
                new LinkedHashMap<>();

        for (Shop shop : shops) {

            result.put(
                    shop.getId(),
                    shop
            );
        }

        return result;
    }

    private FollowingShopResult toResult(
            ShopFollow follow,
            Map<UUID, Shop> shopsById
    ) {
        Shop shop = shopsById.get(follow.getShopId());

        if (shop == null) {

            throw new IllegalStateException(
                    "Referenced shop not found"
            );
        }

        return new FollowingShopResult(
                shop.getId(),
                shop.getName(),
                shop.getSlug(),
                shop.getLogoObjectKey(),
                follow.getFollowedAt()
        );
    }

    private int resolvePage(Integer value) {
        if (value == null) {
            return DEFAULT_PAGE;
        }

        if (value < 1) {
            throw new InvalidShopFollowQueryException();
        }

        return value;
    }

    private int resolveSize(Integer value) {
        if (value == null) {
            return DEFAULT_SIZE;
        }

        if (value < 1 || value > MAX_SIZE) {
            throw new InvalidShopFollowQueryException();
        }

        return value;
    }

    private Sort resolveSort(String value) {
        String raw = value == null
                ? DEFAULT_SORT
                : value.strip();

        if (raw.isEmpty()) {
            throw new InvalidShopFollowQueryException();
        }

        String[] parts = raw.split(",", -1);

        if (parts.length != 2) {
            throw new InvalidShopFollowQueryException();
        }

        String field = parts[0]
                .strip()
                .toLowerCase(Locale.ROOT);

        String direction = parts[1]
                .strip()
                .toLowerCase(Locale.ROOT);

        if (!field.equals("created_at")) {
            throw new InvalidShopFollowQueryException();
        }

        Sort.Direction sortDirection =
                switch (direction) {

                    case "asc" ->
                            Sort.Direction.ASC;

                    case "desc" ->
                            Sort.Direction.DESC;

                    default ->
                            throw new InvalidShopFollowQueryException();
                };

        return Sort.by(
                new Sort.Order(
                        sortDirection,
                        "followedAt"
                ),
                new Sort.Order(
                        sortDirection,
                        "id.shopId"
                )
        );
    }
}