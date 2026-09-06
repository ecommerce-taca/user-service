package com.ecommerce.authuser.kyc.web.complete;

import com.ecommerce.authuser.kyc.domain.KycDocumentStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record CompleteKycDocumentResponse(
        Data data,
        Meta meta
) {

    public record Data(
            @JsonProperty("document_id")
            UUID documentId,

            KycDocumentStatus status,

            @JsonProperty("uploaded_at")
            Instant uploadedAt
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
