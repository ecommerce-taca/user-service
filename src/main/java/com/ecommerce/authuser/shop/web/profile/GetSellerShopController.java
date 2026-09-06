package com.ecommerce.authuser.shop.web.profile;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.shop.application.profile.read.GetSellerShopResult;
import com.ecommerce.authuser.shop.application.profile.read.GetSellerShopService;

import com.ecommerce.authuser.shop.exception.SellerPermissionDeniedException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class GetSellerShopController {

    private final GetSellerShopService getSellerShopService;

    @GetMapping("/shop")
    public ResponseEntity<GetSellerShopResponse> getShop(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {
        UUID userId = parseUserId(jwt.getSubject());

        GetSellerShopResult result = getSellerShopService.get(userId);

        GetSellerShopResponse response =
                new GetSellerShopResponse(
                        new GetSellerShopResponse.Data(
                                result.id(),
                                result.name(),
                                result.slug(),
                                result.businessName(),
                                result.taxCodeMasked(),
                                result.description(),
                                result.logoUrl(),
                                result.status(),
                                result.kycStatus(),
                                mapWarehouseSummary(result.warehouseSummary()),
                                mapBankSummary(result.bankSummary()),
                                result.createdAt(),
                                result.updatedAt()
                        ),
                        new GetSellerShopResponse.Meta(
                                resolveRequestId(requestId)
                        )
                );

        return ResponseEntity.ok(
                response
        );
    }

    private GetSellerShopResponse.WarehouseSummary mapWarehouseSummary(
            GetSellerShopResult.WarehouseSummary value
    ) {

        if (value == null) {
            return null;
        }

        return new GetSellerShopResponse
                .WarehouseSummary(
                value.warehouseName(),
                value.district(),
                value.province()
        );
    }

    private GetSellerShopResponse.BankSummary mapBankSummary(
            GetSellerShopResult.BankSummary value
    ) {

        if (value == null) {
            return null;
        }

        return new GetSellerShopResponse.BankSummary(
                value.bankName(),
                value.maskedAccount(),
                value.verified()
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