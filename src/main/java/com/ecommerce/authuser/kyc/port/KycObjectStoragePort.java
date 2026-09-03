package com.ecommerce.authuser.kyc.port;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public interface KycObjectStoragePort {

    PresignResult presignUpload(
            String objectKey,
            String contentType,
            long sizeBytes,
            String sha256,
            Duration ttl
    );

    record PresignResult(
            String uploadUrl,
            Instant expiresAt,
            Map<String, String> requiredHeaders
    ) {
    }
}
