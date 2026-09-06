package com.ecommerce.authuser.shop.application.profile.read;

import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.ShopStatus;

import java.time.Instant;
import java.util.UUID;

public record GetSellerShopResult(
        UUID id,
        String name,
        String slug,
        String businessName,
        String taxCodeMasked,
        String description,
        String logoUrl,
        ShopStatus status,
        KycStatus kycStatus,
        WarehouseSummary warehouseSummary,
        BankSummary bankSummary,
        Instant createdAt,
        Instant updatedAt
) {

    public record WarehouseSummary(
            String warehouseName,
            String district,
            String province
    ) {
    }

    public record BankSummary(
            String bankName,
            String maskedAccount,
            boolean verified
    ) {
    }
}
