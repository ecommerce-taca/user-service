package com.ecommerce.authuser.outbox.infrastructure.cleanup;

import com.ecommerce.authuser.outbox.application.OutboxCleanupService;
import com.ecommerce.authuser.outbox.application.OutboxCleanupService.OutboxCleanupResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "auth.outbox-cleanup",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanupScheduler.class);

    private final OutboxCleanupService outboxCleanupService;

    public OutboxCleanupScheduler(OutboxCleanupService outboxCleanupService) {
        this.outboxCleanupService = outboxCleanupService;
    }

    @Scheduled(fixedDelayString = "${auth.outbox-cleanup.fixed-delay-ms:3600000}")
    public void cleanupOutboxEvents() {
        try {
            OutboxCleanupResult result = outboxCleanupService.cleanupOnce();

            if (result.totalDeleted() > 0) {
                log.info(
                        "Cleaned up outbox event(s) deleted_published={} deleted_failed={} total_deleted={}",
                        result.deletedPublished(),
                        result.deletedFailed(),
                        result.totalDeleted()
                );
            }

        } catch (RuntimeException ex) {
            log.warn("Outbox cleanup scheduler tick failed", ex);
        }
    }
}