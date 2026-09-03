package com.ecommerce.authuser.shop.web.profile;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.shop.application.profile.update.UpdateSellerShopCommand;
import com.ecommerce.authuser.shop.application.profile.update.UpdateSellerShopResult;
import com.ecommerce.authuser.shop.application.profile.update.UpdateSellerShopService;

import com.ecommerce.authuser.shop.exception.InvalidSellerShopProfileException;
import com.ecommerce.authuser.shop.exception.SellerPermissionDeniedException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class UpdateSellerShopController {

    private final UpdateSellerShopService updateSellerShopService;

    @PutMapping("/shop")
    public ResponseEntity<UpdateSellerShopResponse>
    updateShop(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateSellerShopRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        if (request.invalid()) {
            throw new InvalidSellerShopProfileException();
        }

        UUID userId = parseUserId(jwt.getSubject());

        UpdateSellerShopResult result =
                updateSellerShopService.update(
                        new UpdateSellerShopCommand(
                                userId,
                                request.nameProvided(),
                                request.name(),
                                request.descriptionProvided(),
                                request.description(),
                                request.logoObjectKeyProvided(),
                                request.logoObjectKey()
                        )
                );

        UpdateSellerShopResponse response =
                new UpdateSellerShopResponse(
                        new UpdateSellerShopResponse.Data(
                                result.id(),
                                result.name(),
                                result.slug(),
                                result.businessName(),
                                result.description(),
                                result.logoUrl(),
                                result.status(),
                                result.kycStatus(),
                                result.updatedAt()
                        ),

                        new UpdateSellerShopResponse.Meta(
                                resolveRequestId(requestId)
                        )
                );

        return ResponseEntity.ok(
                response
        );
    }

    private UUID parseUserId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new SellerPermissionDeniedException();
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