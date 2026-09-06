package com.ecommerce.authuser.shop.application.profile.update;

import java.util.UUID;

public record UpdateSellerShopCommand(
        UUID userId,

        boolean nameProvided,
        String name,

        boolean descriptionProvided,
        String description,

        boolean logoObjectKeyProvided,
        String logoObjectKey
) {
}
