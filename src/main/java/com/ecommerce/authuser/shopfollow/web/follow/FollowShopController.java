package com.ecommerce.authuser.shopfollow.web.follow;

import com.ecommerce.authuser.common.id.CanonicalUuidParser;

import com.ecommerce.authuser.common.web.RequestIdResolver;
import com.ecommerce.authuser.shopfollow.application.follow.FollowShopCommand;
import com.ecommerce.authuser.shopfollow.application.follow.FollowShopResult;
import com.ecommerce.authuser.shopfollow.application.follow.FollowShopService;

import com.ecommerce.authuser.shopfollow.exception.InvalidShopFollowInputException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
public class FollowShopController {

    private final FollowShopService followShopService;

    @PostMapping("/{shopId}/follow")
    public ResponseEntity<FollowShopResponse> followShop(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String shopId,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());

        UUID parsedShopId =
                CanonicalUuidParser.parse(
                        shopId,
                        InvalidShopFollowInputException::new
                );

        FollowShopResult result =
                followShopService.follow(
                        new FollowShopCommand(
                                userId,
                                parsedShopId
                        )
                );

        FollowShopResponse response =
                new FollowShopResponse(
                        new FollowShopResponse.Data(
                                result.shopId(),
                                result.followedAt()
                        ),
                        new FollowShopResponse.Meta(
                                RequestIdResolver.resolve(requestId)
                        )
                );

        HttpStatus status =
                result.created()
                        ? HttpStatus.CREATED
                        : HttpStatus.OK;

        return ResponseEntity
                .status(status)
                .body(response);
    }
}
