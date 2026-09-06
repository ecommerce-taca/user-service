package com.ecommerce.authuser.kyc.port;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public interface KycObjectStoragePort {

    PresignResult presignUpload(
            String objectKey,
            String contentType,
            long sizeBytes,
            String sha256,
            Duration ttl
    );

    Optional<ObjectMetadata> findObjectMetadata(
            String objectKey
    );

    record PresignResult(
            String uploadUrl,
            Instant expiresAt,
            Map<String, String> requiredHeaders
    ) {
    }

    record ObjectMetadata(
            long sizeBytes,
            String contentType,
            String sha256
    ) {
    }

    DownloadResult presignDownload(
            String objectKey,
            Duration ttl
    );

    record DownloadResult(
            String downloadUrl,
            Instant expiresAt
    ) {
    }
}
