package com.ecommerce.authuser.address.application.create;

import java.util.UUID;

public record CreateMyAddressCommand(
        UUID userId,
        String recipient,
        String phone,
        String line1,
        String line2,
        String countryCode,
        String provinceCode,
        String wardCode,
        String postalCode,
        boolean defaultRequested
) {
}
