package com.ecommerce.authuser.kyc.infrastructure.storage;

import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.kyc.port.KycObjectStoragePort;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
@Profile("!prod")
public class MockKycObjectStorageAdapter
        implements KycObjectStoragePort {

    @Override
    public PresignResult presignUpload(
            String objectKey,
            String contentType,
            long sizeBytes,
            String sha256,
            Duration ttl
    ) {

        Instant expiresAt = Instant.now().plus(ttl);

        String uploadUrl =
                "https://storage.example/mock-upload/"
                        + UuidV7Generator
                        .generate();

        return new PresignResult(
                uploadUrl,
                expiresAt,
                Map.of(
                        "Content-Type",
                        contentType,
                        "x-content-sha256",
                        sha256
                )
        );
    }
}
