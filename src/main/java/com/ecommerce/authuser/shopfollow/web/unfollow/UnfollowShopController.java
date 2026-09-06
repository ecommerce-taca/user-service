package com.ecommerce.authuser.shopfollow.web.unfollow;

import com.ecommerce.authuser.common.id.CanonicalUuidParser;
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

        UUID parsedShopId =
                CanonicalUuidParser.parse(
                        shopId,
                        InvalidShopFollowInputException::new
                );;

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
}
