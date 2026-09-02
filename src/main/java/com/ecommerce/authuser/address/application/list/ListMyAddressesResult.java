package com.ecommerce.authuser.address.application.list;

import java.util.List;

public record ListMyAddressesResult(
        List<AddressResult> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
