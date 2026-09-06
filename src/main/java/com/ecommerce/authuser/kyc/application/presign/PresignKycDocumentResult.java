package com.ecommerce.authuser.kyc.application.presign;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PresignKycDocumentResult(
        UUID documentId,
        String objectKey,
        String uploadUrl,
        Instant expiresAt,
        Map<String, String> requiredHeaders
) {
}
