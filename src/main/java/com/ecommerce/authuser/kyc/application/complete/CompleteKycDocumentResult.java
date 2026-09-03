package com.ecommerce.authuser.kyc.application.complete;

import com.ecommerce.authuser.kyc.domain.KycDocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record CompleteKycDocumentResult(
        UUID documentId,
        KycDocumentStatus status,
        Instant uploadedAt
) {
}
