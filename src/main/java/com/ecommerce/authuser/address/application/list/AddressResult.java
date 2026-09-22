package com.ecommerce.authuser.address.application.list;

import java.time.Instant;
import java.util.UUID;

public record AddressResult(
        UUID id,
        String recipient,
        String phone,
        String line1,
        String line2,
        String countryCode,
        String provinceCode,
        String province,
        String wardCode,
        String ward,
        String district,
        String postalCode,
        boolean defaultAddress,
        Instant createdAt,
        Instant updatedAt
) {
}
