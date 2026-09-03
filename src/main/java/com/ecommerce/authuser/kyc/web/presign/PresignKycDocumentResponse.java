package com.ecommerce.authuser.kyc.web.presign;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PresignKycDocumentResponse(
        Data data,
        Meta meta
) {

    public record Data(
            @JsonProperty("document_id")
            UUID documentId,

            @JsonProperty("object_key")
            String objectKey,

            @JsonProperty("upload_url")
            String uploadUrl,

            @JsonProperty("expires_at")
            Instant expiresAt,

            @JsonProperty("required_headers")
            Map<String, String> requiredHeaders
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
