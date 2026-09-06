package com.ecommerce.authuser.shop.web.publicprofile;

import com.ecommerce.authuser.common.id.CanonicalUuidParser;

import com.ecommerce.authuser.common.web.RequestIdResolver;
import com.ecommerce.authuser.shop.application.publicprofile.GetPublicShopQuery;
import com.ecommerce.authuser.shop.application.publicprofile.GetPublicShopResult;
import com.ecommerce.authuser.shop.application.publicprofile.GetPublicShopService;

import com.ecommerce.authuser.shop.exception.InvalidPublicShopQueryException;

import com.ecommerce.authuser.shop.port.ShopAssetUrlPort;

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
public class GetPublicShopController {

    private final GetPublicShopService getPublicShopService;

    private final ShopAssetUrlPort shopAssetUrlPort;

    @GetMapping("/{shopId}")
    public ResponseEntity<GetPublicShopResponse> getPublicShop(
            @PathVariable String shopId,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        UUID parsedShopId =
                CanonicalUuidParser.parse(
                        shopId,
                        InvalidPublicShopQueryException::new
                );

        GetPublicShopResult result =
                getPublicShopService.get(
                        new GetPublicShopQuery(
                                parsedShopId
                        )
                );

        String logoUrl =
                shopAssetUrlPort.resolveLogoUrl(
                        result.logoObjectKey()
                );

        GetPublicShopResponse response =
                new GetPublicShopResponse(
                        new GetPublicShopResponse.Data(
                                result.shopId(),
                                result.name(),
                                result.slug(),
                                logoUrl,
                                result.description(),
                                result.verified(),
                                result.status(),
                                result.followerCount(),
                                result.createdAt()
                        ),
                        new GetPublicShopResponse.Meta(
                                RequestIdResolver.resolve(requestId)
                        )
                );

        return ResponseEntity.ok(
                response
        );
    }
}
