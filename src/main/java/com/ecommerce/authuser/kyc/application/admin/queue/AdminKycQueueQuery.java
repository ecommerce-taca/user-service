package com.ecommerce.authuser.kyc.application.admin.queue;

import com.ecommerce.authuser.shop.domain.KycStatus;

public record AdminKycQueueQuery(
        KycStatus status,
        int page,
        int size,
        String sortField,
        boolean ascending,
        String q
) {
}
