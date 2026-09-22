package com.ecommerce.authuser.outbox.infrastructure.health;

import com.ecommerce.authuser.outbox.infrastructure.kafka.OutboxPublisherProperties;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxLagHealthIndicatorTest {

    private final OutboxEventRepository outboxEventRepository =
            mock(OutboxEventRepository.class);

    @Test
    void health_shouldBeUpWhenOutboxLagIsWithinThresholds() {
        OutboxPublisherProperties properties = properties();

        when(outboxEventRepository.countReadyToPublish(any(Instant.class)))
                .thenReturn(10L);
        when(outboxEventRepository.countWaitingForRetry(any(Instant.class)))
                .thenReturn(2L);
        when(outboxEventRepository.countFailed())
                .thenReturn(0L);
        when(outboxEventRepository.findOldestReadyToPublishCreatedAt(any(Instant.class)))
                .thenReturn(Optional.of(Instant.now().minusSeconds(30)));

        Health health = new OutboxLagHealthIndicator(
                outboxEventRepository,
                properties
        ).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("enabled", true)
                .containsEntry("ready_to_publish", 10L)
                .containsEntry("waiting_for_retry", 2L)
                .containsEntry("failed", 0L);
    }

    @Test
    void health_shouldBeDownWhenReadyPendingCountExceedsThreshold() {
        OutboxPublisherProperties properties = properties();
        properties.setMaxPendingEvents(1);

        when(outboxEventRepository.countReadyToPublish(any(Instant.class)))
                .thenReturn(2L);
        when(outboxEventRepository.countWaitingForRetry(any(Instant.class)))
                .thenReturn(0L);
        when(outboxEventRepository.countFailed())
                .thenReturn(0L);
        when(outboxEventRepository.findOldestReadyToPublishCreatedAt(any(Instant.class)))
                .thenReturn(Optional.empty());

        Health health = new OutboxLagHealthIndicator(
                outboxEventRepository,
                properties
        ).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("ready_to_publish", 2L)
                .containsEntry("max_pending_events", 1L);
    }

    @Test
    void health_shouldBeDownWhenOldestReadyEventIsTooOld() {
        OutboxPublisherProperties properties = properties();
        properties.setMaxOldestPendingAgeSeconds(300);

        when(outboxEventRepository.countReadyToPublish(any(Instant.class)))
                .thenReturn(1L);
        when(outboxEventRepository.countWaitingForRetry(any(Instant.class)))
                .thenReturn(0L);
        when(outboxEventRepository.countFailed())
                .thenReturn(0L);
        when(outboxEventRepository.findOldestReadyToPublishCreatedAt(any(Instant.class)))
                .thenReturn(Optional.of(Instant.now().minusSeconds(600)));

        Health health = new OutboxLagHealthIndicator(
                outboxEventRepository,
                properties
        ).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void health_shouldBeDownWhenFailedEventsExceedThreshold() {
        OutboxPublisherProperties properties = properties();
        properties.setMaxFailedEvents(0);

        when(outboxEventRepository.countReadyToPublish(any(Instant.class)))
                .thenReturn(0L);
        when(outboxEventRepository.countWaitingForRetry(any(Instant.class)))
                .thenReturn(0L);
        when(outboxEventRepository.countFailed())
                .thenReturn(1L);
        when(outboxEventRepository.findOldestReadyToPublishCreatedAt(any(Instant.class)))
                .thenReturn(Optional.empty());

        Health health = new OutboxLagHealthIndicator(
                outboxEventRepository,
                properties
        ).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("failed", 1L)
                .containsEntry("max_failed_events", 0L);
    }

    @Test
    void health_shouldBeUpWithoutQueryingRepositoryWhenPublisherDisabled() {
        OutboxPublisherProperties properties = properties();
        properties.setEnabled(false);

        Health health = new OutboxLagHealthIndicator(
                outboxEventRepository,
                properties
        ).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("enabled", false);

        verifyNoInteractions(outboxEventRepository);
    }

    private static OutboxPublisherProperties properties() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        properties.setEnabled(true);
        properties.setMaxPendingEvents(1000);
        properties.setMaxOldestPendingAgeSeconds(300);
        properties.setMaxFailedEvents(0);
        return properties;
    }
}