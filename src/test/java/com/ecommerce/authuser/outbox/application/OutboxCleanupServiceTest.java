package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.infrastructure.cleanup.OutboxCleanupProperties;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxCleanupServiceTest {

    private final OutboxEventRepository outboxEventRepository =
            mock(OutboxEventRepository.class);

    @Test
    void cleanupOnce_shouldDeletePublishedAndFailedEventsWithinBatch() {
        OutboxCleanupProperties properties = properties();

        List<UUID> publishedIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        List<UUID> failedIds = List.of(UUID.randomUUID());

        when(outboxEventRepository.findPublishedIdsForCleanup(
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(publishedIds);

        when(outboxEventRepository.findFailedIdsForCleanup(
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(failedIds);

        when(outboxEventRepository.deleteAllByIdIn(publishedIds))
                .thenReturn(2);
        when(outboxEventRepository.deleteAllByIdIn(failedIds))
                .thenReturn(1);

        OutboxCleanupService.OutboxCleanupResult result =
                new OutboxCleanupService(
                        outboxEventRepository,
                        properties
                ).cleanupOnce();

        assertThat(result.deletedPublished()).isEqualTo(2);
        assertThat(result.deletedFailed()).isEqualTo(1);
        assertThat(result.totalDeleted()).isEqualTo(3);

        verify(outboxEventRepository).deleteAllByIdIn(publishedIds);
        verify(outboxEventRepository).deleteAllByIdIn(failedIds);
    }

    @Test
    void cleanupOnce_shouldNotCallDeleteWhenNoIdsFound() {
        OutboxCleanupProperties properties = properties();

        when(outboxEventRepository.findPublishedIdsForCleanup(
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(List.of());

        when(outboxEventRepository.findFailedIdsForCleanup(
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(List.of());

        OutboxCleanupService.OutboxCleanupResult result =
                new OutboxCleanupService(
                        outboxEventRepository,
                        properties
                ).cleanupOnce();

        assertThat(result.totalDeleted()).isZero();

        verify(outboxEventRepository, never()).deleteAllByIdIn(any());
    }

    @Test
    void cleanupOnce_shouldSkipWhenCleanupDisabled() {
        OutboxCleanupProperties properties = properties();
        properties.setEnabled(false);

        OutboxCleanupService.OutboxCleanupResult result =
                new OutboxCleanupService(
                        outboxEventRepository,
                        properties
                ).cleanupOnce();

        assertThat(result.totalDeleted()).isZero();

        verifyNoInteractions(outboxEventRepository);
    }

    private static OutboxCleanupProperties properties() {
        OutboxCleanupProperties properties = new OutboxCleanupProperties();
        properties.setEnabled(true);
        properties.setPublishedRetentionDays(7);
        properties.setFailedRetentionDays(30);
        properties.setBatchSize(500);
        return properties;
    }
}