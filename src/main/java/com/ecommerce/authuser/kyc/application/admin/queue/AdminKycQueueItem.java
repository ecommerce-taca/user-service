package com.ecommerce.authuser.kyc.application.admin.queue;

import com.ecommerce.authuser.shop.domain.KycStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminKycQueueItem(
        UUID shopId,
        String shopName,
        UUID ownerUserId,
        UUID kycCaseId,
        KycStatus status,
        Instant submittedAt,
        long documentCount,
        double ageHours
) {
}
