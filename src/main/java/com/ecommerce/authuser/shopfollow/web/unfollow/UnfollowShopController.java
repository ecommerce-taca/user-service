package com.ecommerce.authuser.shopfollow.web.unfollow;

import com.ecommerce.authuser.shopfollow.application.unfollow.UnfollowShopCommand;
import com.ecommerce.authuser.shopfollow.application.unfollow.UnfollowShopService;
import com.ecommerce.authuser.shopfollow.exception.InvalidShopFollowInputException;

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
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
public class UnfollowShopController {

    private final UnfollowShopService unfollowShopService;

    @DeleteMapping("/{shopId}/follow")
    public ResponseEntity<Void> unfollowShop(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String shopId
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());

        UUID parsedShopId = parseShopId(shopId);

        unfollowShopService.unfollow(
                new UnfollowShopCommand(
                        userId,
                        parsedShopId
                )
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    private UUID parseShopId(String value) {

        if (value == null || value.isBlank()) {
            throw new InvalidShopFollowInputException();
        }

        String normalized = value.strip();

        try {

            UUID shopId = UUID.fromString(normalized);

            if (!shopId
                    .toString()
                    .equalsIgnoreCase(normalized)) {
                throw new InvalidShopFollowInputException();
            }

            return shopId;

        } catch (IllegalArgumentException ex) {
            throw new InvalidShopFollowInputException();
        }
    }
}
