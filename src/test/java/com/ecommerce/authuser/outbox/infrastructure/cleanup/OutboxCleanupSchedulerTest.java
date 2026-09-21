package com.ecommerce.authuser.outbox.infrastructure.cleanup;

import com.ecommerce.authuser.outbox.application.OutboxCleanupService;
import com.ecommerce.authuser.outbox.application.OutboxCleanupService.OutboxCleanupResult;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class OutboxCleanupSchedulerTest {

    private final OutboxCleanupService outboxCleanupService =
            mock(OutboxCleanupService.class);

    @Test
    void cleanupOutboxEvents_shouldCallCleanupService() {
        when(outboxCleanupService.cleanupOnce())
                .thenReturn(new OutboxCleanupResult(2, 1));

        OutboxCleanupScheduler scheduler =
                new OutboxCleanupScheduler(outboxCleanupService);

        scheduler.cleanupOutboxEvents();

        verify(outboxCleanupService).cleanupOnce();
    }

    @Test
    void cleanupOutboxEvents_shouldSwallowCleanupException() {
        doThrow(new IllegalStateException("DB unavailable"))
                .when(outboxCleanupService)
                .cleanupOnce();

        OutboxCleanupScheduler scheduler =
                new OutboxCleanupScheduler(outboxCleanupService);

        scheduler.cleanupOutboxEvents();

        verify(outboxCleanupService).cleanupOnce();
    }
}