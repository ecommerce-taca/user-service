package com.ecommerce.authuser.shopfollow.web.list;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.common.web.RequestIdResolver;
import com.ecommerce.authuser.shop.port.ShopAssetUrlPort;

import com.ecommerce.authuser.shopfollow.application.list.FollowingShopResult;
import com.ecommerce.authuser.shopfollow.application.list.ListFollowingQuery;
import com.ecommerce.authuser.shopfollow.application.list.ListFollowingResult;
import com.ecommerce.authuser.shopfollow.application.list.ListFollowingService;
import com.ecommerce.authuser.shopfollow.exception.InvalidShopFollowQueryException;

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
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class ListFollowingController {

    private final ListFollowingService listFollowingService;

    private final ShopAssetUrlPort shopAssetUrlPort;

    @GetMapping("/following")
    public ResponseEntity<ListFollowingResponse> listFollowing(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());

        ListFollowingResult result =
                listFollowingService.list(
                        new ListFollowingQuery(
                                userId,
                                parseInteger(page),
                                parseInteger(size),
                                sort
                        )
                );

        List<ListFollowingResponse.Data> data =
                result.items()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        ListFollowingResponse response =
                new ListFollowingResponse(
                        data,
                        new ListFollowingResponse.Meta(
                                result.page(),
                                result.size(),
                                result.total(),
                                result.totalPages(),
                                RequestIdResolver.resolve(requestId)
                        )
                );

        return ResponseEntity.ok(
                response
        );
    }

    private ListFollowingResponse.Data toResponse(
            FollowingShopResult item
    ) {

        String logoUrl = shopAssetUrlPort
                .resolveLogoUrl(item.logoObjectKey());

        return new ListFollowingResponse.Data(
                item.shopId(),
                item.name(),
                item.slug(),
                logoUrl,
                item.followedAt()
        );
    }

    private Integer parseInteger(String value) {

        if (value == null) {
            return null;
        }

        if (value.isBlank()) {
            throw new InvalidShopFollowQueryException();
        }

        try {
            return Integer.valueOf(value.strip());
        } catch (NumberFormatException ex) {
            throw new InvalidShopFollowQueryException();
        }
    }
}
