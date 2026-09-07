package com.ecommerce.authuser.kyc.web.admin.detail;

import com.ecommerce.authuser.kyc.domain.KycDocumentStatus;
import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.ShopStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminKycDetailResponse(
        Data data,
        Meta meta
) {

    public record Data(
            ShopData shop,

            @JsonProperty("kyc_case")
            KycCaseData kycCase
    ) {
    }

    public record ShopData(
            UUID id,
            String name,

            @JsonProperty("business_name")
            String businessName,

            @JsonProperty("tax_code_masked")
            String taxCodeMasked,

            ShopStatus status
    ) {
    }

    public record KycCaseData(
            UUID id,
            KycStatus status,

            @JsonProperty("submitted_at")
            Instant submittedAt,

            List<DocumentData> documents
    ) {
    }

    public record DocumentData(
            UUID id,

            @JsonProperty("document_type")
            String documentType,

            @JsonProperty("original_file_name")
            String originalFileName,

            @JsonProperty("content_type")
            String contentType,

            @JsonProperty("size_bytes")
            int sizeBytes,

            KycDocumentStatus status,

            @JsonProperty("download_url")
            String downloadUrl,

            @JsonProperty("download_expires_at")
            Instant downloadExpiresAt
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
