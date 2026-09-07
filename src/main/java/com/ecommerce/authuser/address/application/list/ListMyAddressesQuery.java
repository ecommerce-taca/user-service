package com.ecommerce.authuser.address.application.list;

import java.util.UUID;

public record ListMyAddressesQuery(
        UUID userId,
        Integer page,
        Integer size,
        String sort
) {
}
