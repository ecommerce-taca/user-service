package com.ecommerce.authuser.kyc.application.presign;

import java.util.UUID;

public record PresignKycDocumentCommand(
        UUID userId,
        String documentType,
        String fileName,
        String contentType,
        long sizeBytes,
        String sha256
) {
}
