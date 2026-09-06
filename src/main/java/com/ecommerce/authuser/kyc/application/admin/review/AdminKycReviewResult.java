package com.ecommerce.authuser.kyc.application.admin.review;

import com.ecommerce.authuser.shop.domain.KycStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminKycReviewResult(
        UUID shopId,
        UUID kycCaseId,
        KycStatus status,
        Instant reviewedAt
) {
}
