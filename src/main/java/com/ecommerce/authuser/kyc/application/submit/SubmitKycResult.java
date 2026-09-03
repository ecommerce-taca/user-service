package com.ecommerce.authuser.kyc.application.submit;

import com.ecommerce.authuser.shop.domain.KycStatus;

import java.time.Instant;
import java.util.UUID;

public record SubmitKycResult(
        UUID shopId,
        UUID kycCaseId,
        KycStatus status,
        Instant submittedAt
) {
}
