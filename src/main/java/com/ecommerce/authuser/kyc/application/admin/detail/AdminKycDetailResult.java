package com.ecommerce.authuser.kyc.application.admin.detail;

import com.ecommerce.authuser.kyc.domain.KycDocumentStatus;
import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.ShopStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminKycDetailResult(
        ShopData shop,
        KycCaseData kycCase
) {

    public record ShopData(
            UUID id,
            String name,
            String businessName,
            String taxCodeMasked,
            ShopStatus status
    ) {
    }

    public record KycCaseData(
            UUID id,
            KycStatus status,
            Instant submittedAt,
            List<DocumentData> documents
    ) {
    }

    public record DocumentData(
            UUID id,
            String documentType,
            String originalFileName,
            String contentType,
            int sizeBytes,
            KycDocumentStatus status,
            String downloadUrl,
            Instant downloadExpiresAt
    ) {
    }
}
