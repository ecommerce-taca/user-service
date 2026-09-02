package com.ecommerce.authuser.shop.application.onboarding.profile;

import java.util.UUID;

public record UpdateSellerProfileCommand(
        UUID userId,
        String name,
        String businessName,
        String taxCode,
        String description,
        String logoObjectKey
) {
}