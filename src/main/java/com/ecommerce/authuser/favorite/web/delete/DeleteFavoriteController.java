package com.ecommerce.authuser.favorite.web.delete;

import com.ecommerce.authuser.favorite.application.delete.DeleteFavoriteCommand;
import com.ecommerce.authuser.favorite.application.delete.DeleteFavoriteService;
import com.ecommerce.authuser.favorite.exception.InvalidFavoriteInputException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/favorites")
@RequiredArgsConstructor
public class DeleteFavoriteController {

    private final DeleteFavoriteService deleteFavoriteService;

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String productId
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());

        UUID parsedProductId = parseProductId(productId);

        deleteFavoriteService.delete(
                new DeleteFavoriteCommand(
                        userId,
                        parsedProductId
                )
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    private UUID parseProductId(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidFavoriteInputException();
        }

        String normalized = value.strip();

        try {
            UUID productId = UUID.fromString(normalized);

            if (!productId
                    .toString()
                    .equalsIgnoreCase(normalized)) {

                throw new InvalidFavoriteInputException();
            }

            return productId;

        } catch (IllegalArgumentException ex) {

            throw new InvalidFavoriteInputException();
        }
    }
}
