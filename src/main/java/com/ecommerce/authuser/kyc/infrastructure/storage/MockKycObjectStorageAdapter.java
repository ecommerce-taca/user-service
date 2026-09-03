package com.ecommerce.authuser.kyc.infrastructure.storage;

import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.kyc.port.KycObjectStoragePort;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("!prod")
public class MockKycObjectStorageAdapter implements KycObjectStoragePort {

    private final Map<String, ObjectMetadata> objects = new ConcurrentHashMap<>();

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
                        + UuidV7Generator.generate();

        objects.put(
                objectKey,
                new ObjectMetadata(
                        sizeBytes,
                        contentType,
                        sha256
                )
        );

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

    @Override
    public Optional<ObjectMetadata> findObjectMetadata(String objectKey) {
        if (objectKey == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                objects.get(objectKey)
        );
    }
}
