package com.ecommerce.authuser.shop.application.profile.update;

import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.ShopStatus;

import java.time.Instant;
import java.util.UUID;

public record UpdateSellerShopResult(
        UUID id,
        String name,
        String slug,
        String businessName,
        String description,
        String logoUrl,
        ShopStatus status,
        KycStatus kycStatus,
        Instant updatedAt
) {
}
