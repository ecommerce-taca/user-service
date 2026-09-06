package com.ecommerce.authuser.kyc.application.complete;

import java.util.UUID;

public record CompleteKycDocumentCommand(
        UUID userId,
        UUID documentId,
        String objectKey,
        long sizeBytes,
        String contentType,
        String sha256
) {
}
