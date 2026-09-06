package com.ecommerce.authuser.shop.port;

import java.util.Optional;

public interface BankCatalogPort {

    Optional<BankInfo> findByCode(
            String bankCode
    );

    record BankInfo(
            String code,
            String name
    ) {
    }
}
