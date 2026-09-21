package com.ecommerce.authuser.outbox.infrastructure.health;

import com.ecommerce.authuser.outbox.infrastructure.kafka.OutboxPublisherProperties;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component("outboxLag")
public class OutboxLagHealthIndicator implements HealthIndicator {

    private final OutboxEventRepository outboxEventRepository;

    private final OutboxPublisherProperties properties;

    public OutboxLagHealthIndicator(
            OutboxEventRepository outboxEventRepository,
            OutboxPublisherProperties properties
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.properties = properties;
    }

    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.up()
                    .withDetail("enabled", false)
                    .build();
        }

        Instant now = Instant.now();

        long readyToPublish = outboxEventRepository.countReadyToPublish(now);
        long waitingForRetry = outboxEventRepository.countWaitingForRetry(now);
        long failed = outboxEventRepository.countFailed();

        Optional<Instant> oldestReadyCreatedAt =
                outboxEventRepository.findOldestReadyToPublishCreatedAt(now);

        long oldestReadyAgeSeconds = oldestReadyCreatedAt
                .map(createdAt -> Duration.between(createdAt, now).toSeconds())
                .map(age -> Math.max(age, 0L))
                .orElse(0L);

        boolean unhealthy = readyToPublish > properties.getMaxPendingEvents()
                || oldestReadyAgeSeconds > properties.getMaxOldestPendingAgeSeconds()
                || failed > properties.getMaxFailedEvents();

        Health.Builder builder = unhealthy ? Health.down() : Health.up();

        return builder
                .withDetail("enabled", true)
                .withDetail("ready_to_publish", readyToPublish)
                .withDetail("waiting_for_retry", waitingForRetry)
                .withDetail("failed", failed)
                .withDetail("oldest_ready_age_seconds", oldestReadyAgeSeconds)
                .withDetail("max_pending_events", properties.getMaxPendingEvents())
                .withDetail(
                        "max_oldest_pending_age_seconds",
                        properties.getMaxOldestPendingAgeSeconds()
                )
                .withDetail("max_failed_events", properties.getMaxFailedEvents())
                .build();
    }
}