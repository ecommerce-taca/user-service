package com.ecommerce.authuser.shopfollow.web.count;

import com.ecommerce.authuser.common.id.CanonicalUuidParser;

import com.ecommerce.authuser.common.web.RequestIdResolver;
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

        UUID parsedShopId =
                CanonicalUuidParser.parse(
                        shopId,
                        InvalidShopFollowQueryException::new
                );

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
                                RequestIdResolver.resolve(requestId)
                        )
                );

        return ResponseEntity.ok(response);
    }
}
