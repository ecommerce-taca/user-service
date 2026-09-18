package com.ecommerce.authuser.outbox.infrastructure.kafka;

import com.ecommerce.authuser.outbox.application.OutboxPublisherService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "auth.outbox-publisher",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);

    private final OutboxPublisherService outboxPublisherService;

    public OutboxPublisherScheduler(OutboxPublisherService outboxPublisherService) {
        this.outboxPublisherService = outboxPublisherService;
    }

    @Scheduled(fixedDelayString = "${auth.outbox-publisher.fixed-delay-ms:2000}")
    public void publishPendingOutboxEvents() {
        try {
            int processedCount = outboxPublisherService.publishPendingBatch();

            if (processedCount > 0) {
                log.info("Processed {} outbox event(s)", processedCount);
            }

        } catch (RuntimeException ex) {
            log.warn("Outbox publisher scheduler tick failed", ex);
        }
    }
}