package com.ecommerce.authuser.favorite.web.contains;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.favorite.application.contains.ContainsFavoritesQuery;
import com.ecommerce.authuser.favorite.application.contains.ContainsFavoritesResult;
import com.ecommerce.authuser.favorite.application.contains.ContainsFavoritesService;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/favorites")
@RequiredArgsConstructor
public class ContainsFavoritesController {

    private static final int MAX_PRODUCT_IDS = 100;

    private final ContainsFavoritesService containsFavoritesService;

    @GetMapping("/contains")
    public ResponseEntity<ContainsFavoritesResponse> containsFavorites(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "product_ids", required = false) String productIds,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());

        List<UUID> parsedProductIds = parseProductIds(productIds);

        ContainsFavoritesResult result =
                containsFavoritesService.contains(
                        new ContainsFavoritesQuery(
                                userId,
                                parsedProductIds
                        )
                );

        Map<String, Boolean> data = new LinkedHashMap<>();

        result.statuses()
                .forEach(
                        (productId, contained) ->
                                data.put(
                                        productId.toString(),
                                        contained
                                )
                );

        ContainsFavoritesResponse response =
                new ContainsFavoritesResponse(
                        Collections.unmodifiableMap(
                                new LinkedHashMap<>(
                                        data
                                )
                        ),
                        new ContainsFavoritesResponse.Meta(
                                resolveRequestId(
                                        requestId
                                )
                        )
                );

        return ResponseEntity.ok(
                response
        );
    }

    private List<UUID> parseProductIds(String value) {

        if (value == null || value.isBlank()) {

            throw new InvalidFavoriteQueryException();
        }

        String[] parts = value.split(",", -1);

        if (parts.length > MAX_PRODUCT_IDS) {
            throw new InvalidFavoriteQueryException();
        }

        List<UUID> productIds = new ArrayList<>(parts.length);

        for (String part : parts) {
            String normalized = part.strip();

            if (normalized.isEmpty()) {
                throw new InvalidFavoriteQueryException();
            }

            productIds.add(parseCanonicalUuid(normalized));
        }

        return List.copyOf(productIds);
    }

    private UUID parseCanonicalUuid(String value) {

        try {
            UUID productId = UUID.fromString(value);

            if (!productId
                    .toString()
                    .equalsIgnoreCase(value)) {

                throw new InvalidFavoriteQueryException();
            }

            return productId;

        } catch (IllegalArgumentException ex) {
            throw new InvalidFavoriteQueryException();
        }
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