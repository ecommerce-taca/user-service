package com.ecommerce.authuser.favorite.application.list;

import com.ecommerce.authuser.favorite.domain.UserFavorite;
import com.ecommerce.authuser.favorite.exception.InvalidFavoriteQueryException;
import com.ecommerce.authuser.favorite.repository.UserFavoriteRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ListMyFavoritesService {

    private static final int DEFAULT_PAGE = 1;

    private static final int DEFAULT_SIZE = 20;

    private static final int MAX_SIZE = 100;

    private static final String DEFAULT_SORT = "created_at,desc";

    private final UserFavoriteRepository userFavoriteRepository;

    @Transactional(readOnly = true)
    public ListMyFavoritesResult list(ListMyFavoritesQuery query) {

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

        Pageable pageable =
                PageRequest.of(
                        page - 1,
                        size,
                        sort
                );

        Page<UserFavorite> resultPage = userFavoriteRepository
                .findAllById_UserId(
                        query.userId(),
                        pageable
                );

        List<FavoriteResult> items =
                resultPage
                        .getContent()
                        .stream()
                        .map(this::toResult)
                        .toList();

        return new ListMyFavoritesResult(
                items,
                page,
                size,
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    private int resolvePage(Integer value) {
        if (value == null) {
            return DEFAULT_PAGE;
        }

        if (value < 1) {
            throw new InvalidFavoriteQueryException();
        }

        return value;
    }

    private int resolveSize(Integer value) {
        if (value == null) {
            return DEFAULT_SIZE;
        }

        if (value < 1 || value > MAX_SIZE) {
            throw new InvalidFavoriteQueryException();
        }

        return value;
    }

    private Sort resolveSort(String value) {
        String raw = value == null ? DEFAULT_SORT : value.trim();

        if (raw.isEmpty()) {
            throw new InvalidFavoriteQueryException();
        }

        String[] parts = raw.split(",", -1);

        if (parts.length != 2) {
            throw new InvalidFavoriteQueryException();
        }

        String field = parts[0]
                .trim()
                .toLowerCase(Locale.ROOT);

        String direction = parts[1]
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!field.equals("created_at")) {
            throw new InvalidFavoriteQueryException();
        }

        Sort.Direction sortDirection =
                switch (direction) {

                    case "asc" ->
                            Sort.Direction.ASC;

                    case "desc" ->
                            Sort.Direction.DESC;

                    default ->
                            throw new InvalidFavoriteQueryException();
                };

        return Sort.by(
                new Sort.Order(
                        sortDirection,
                        "createdAt"
                ),
                new Sort.Order(
                        sortDirection,
                        "id.productId"
                )
        );
    }

    private FavoriteResult toResult(UserFavorite favorite) {
        return new FavoriteResult(
                favorite.getProductId(),
                favorite.getCreatedAt()
        );
    }
}