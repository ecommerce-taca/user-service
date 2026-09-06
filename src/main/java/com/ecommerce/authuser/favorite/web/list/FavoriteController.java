package com.ecommerce.authuser.favorite.web.list;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.favorite.application.list.FavoriteResult;
import com.ecommerce.authuser.favorite.application.list.ListMyFavoritesQuery;
import com.ecommerce.authuser.favorite.application.list.ListMyFavoritesResult;
import com.ecommerce.authuser.favorite.application.list.ListMyFavoritesService;
import com.ecommerce.authuser.favorite.exception.InvalidFavoriteQueryException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final ListMyFavoritesService listMyFavoritesService;

    @GetMapping
    public ResponseEntity<ListMyFavoritesResponse> listMyFavorites(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());

        ListMyFavoritesResult result =
                listMyFavoritesService.list(
                        new ListMyFavoritesQuery(
                                userId,
                                parseInteger(page),
                                parseInteger(size),
                                sort
                        )
                );

        List<ListMyFavoritesResponse.Data> items =
                result.items()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        ListMyFavoritesResponse response =
                new ListMyFavoritesResponse(
                        items,

                        new ListMyFavoritesResponse.Meta(
                                result.page(),
                                result.size(),
                                result.total(),
                                result.totalPages(),
                                resolveRequestId(
                                        requestId
                                )
                        )
                );

        return ResponseEntity.ok(
                response
        );
    }

    private Integer parseInteger(String value) {
        if (value == null) {
            return null;
        }

        if (value.isBlank()) {
            throw new InvalidFavoriteQueryException();
        }

        try {
            return Integer.valueOf(value.strip());
        } catch (NumberFormatException ex) {
            throw new InvalidFavoriteQueryException();
        }
    }

    private ListMyFavoritesResponse.Data toResponse(
            FavoriteResult favorite
    ) {

        return new ListMyFavoritesResponse.Data(
                favorite.productId(),
                favorite.createdAt()
        );
    }

    private String resolveRequestId(String requestId) {
        if (requestId != null
                && !requestId.isBlank()
                && requestId.length() <= 64) {

            return requestId;
        }

        return UuidV7Generator
                .generate()
                .toString();
    }
}