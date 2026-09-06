package com.ecommerce.authuser.shopfollow.web.count;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.shopfollow.application.count.GetFollowerCountQuery;
import com.ecommerce.authuser.shopfollow.application.count.GetFollowerCountResult;
import com.ecommerce.authuser.shopfollow.application.count.GetFollowerCountService;
import com.ecommerce.authuser.shopfollow.exception.InvalidShopFollowQueryException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
public class GetFollowerCountController {

    private final GetFollowerCountService getFollowerCountService;

    @GetMapping("/{shopId}/followers/count")
    public ResponseEntity<GetFollowerCountResponse> getFollowerCount(
            @PathVariable String shopId,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        UUID parsedShopId = parseCanonicalUuid(shopId);

        GetFollowerCountResult result =
                getFollowerCountService.get(
                        new GetFollowerCountQuery(
                                parsedShopId
                        )
                );

        GetFollowerCountResponse response =
                new GetFollowerCountResponse(
                        new GetFollowerCountResponse.Data(
                                result.shopId().toString(),
                                result.followerCount()
                        ),
                        new GetFollowerCountResponse.Meta(
                                resolveRequestId(requestId)
                        )
                );

        return ResponseEntity.ok(response);
    }

    private UUID parseCanonicalUuid(String value) {

        if (value == null || value.isBlank()) {
            throw new InvalidShopFollowQueryException();
        }

        String normalized = value.strip();

        try {
            UUID shopId = UUID.fromString(normalized);

            if (!shopId
                    .toString()
                    .equalsIgnoreCase(normalized)) {

                throw new InvalidShopFollowQueryException();
            }

            return shopId;

        } catch (IllegalArgumentException ex) {
            throw new InvalidShopFollowQueryException();
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
