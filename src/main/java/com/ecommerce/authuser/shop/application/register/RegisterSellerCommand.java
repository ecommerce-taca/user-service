package com.ecommerce.authuser.shop.application.register;

import java.util.UUID;

public record RegisterSellerCommand(
        UUID userId,
        String name,
        String businessName,
        String taxCode,
        String slug,
        String description
) {
}
