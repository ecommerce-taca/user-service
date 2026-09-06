package com.ecommerce.authuser.address.application.delete;

import java.util.UUID;

public record DeleteMyAddressCommand(
        UUID userId,
        UUID addressId
) {
}
