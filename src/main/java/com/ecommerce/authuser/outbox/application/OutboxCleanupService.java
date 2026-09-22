package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.infrastructure.cleanup.OutboxCleanupProperties;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxCleanupService {

    private final OutboxEventRepository outboxEventRepository;

    private final OutboxCleanupProperties properties;

    public OutboxCleanupService(
            OutboxEventRepository outboxEventRepository,
            OutboxCleanupProperties properties
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.properties = properties;
    }

    @Transactional
    public OutboxCleanupResult cleanupOnce() {
        if (!properties.isEnabled()) {
            return new OutboxCleanupResult(0, 0);
        }

        Instant now = Instant.now();

        Instant publishedCutoff = now.minus(
                Duration.ofDays(properties.getPublishedRetentionDays())
        );
        Instant failedCutoff = now.minus(
                Duration.ofDays(properties.getFailedRetentionDays())
        );

        int deletedPublished = deletePublished(publishedCutoff);
        int deletedFailed = deleteFailed(failedCutoff);

        return new OutboxCleanupResult(deletedPublished, deletedFailed);
    }

    private int deletePublished(Instant cutoff) {
        List<UUID> ids = outboxEventRepository.findPublishedIdsForCleanup(
                cutoff,
                PageRequest.of(0, properties.getBatchSize())
        );

        if (ids.isEmpty()) {
            return 0;
        }

        return outboxEventRepository.deleteAllByIdIn(ids);
    }

    private int deleteFailed(Instant cutoff) {
        List<UUID> ids = outboxEventRepository.findFailedIdsForCleanup(
                cutoff,
                PageRequest.of(0, properties.getBatchSize())
        );

        if (ids.isEmpty()) {
            return 0;
        }

        return outboxEventRepository.deleteAllByIdIn(ids);
    }

    public record OutboxCleanupResult(
            int deletedPublished,
            int deletedFailed
    ) {
        public int totalDeleted() {
            return deletedPublished + deletedFailed;
        }
    }
}