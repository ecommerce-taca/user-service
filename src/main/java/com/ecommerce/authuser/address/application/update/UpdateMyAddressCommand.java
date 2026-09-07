package com.ecommerce.authuser.address.application.update;

import java.util.UUID;

public record UpdateMyAddressCommand(
        UUID userId,
        UUID addressId,
        String recipient,
        String phone,
        String line1,
        String line2,
        String ward,
        String district,
        String province,
        String postalCode,
        boolean defaultProvided,
        boolean defaultRequested
) {
}
