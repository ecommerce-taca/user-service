package com.ecommerce.authuser.shop.application.onboarding.warehouse;

import java.util.List;
import java.util.UUID;

public record UpdateSellerWarehouseCommand(
        UUID userId,
        String warehouseName,
        String contactName,
        String contactPhone,
        AddressCommand address,
        List<String> carrierPreferences,
        Boolean codEnabled
) {

    public record AddressCommand(
            String line1,
            String line2,
            String ward,
            String district,
            String province,
            String postalCode
    ) {
    }
}