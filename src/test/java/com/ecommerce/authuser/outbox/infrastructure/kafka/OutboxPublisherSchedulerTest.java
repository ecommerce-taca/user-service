package com.ecommerce.authuser.outbox.infrastructure.kafka;

import com.ecommerce.authuser.outbox.application.OutboxPublisherService;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherSchedulerTest {

    private final OutboxPublisherService outboxPublisherService = mock(OutboxPublisherService.class);

    @Test
    void publishPendingOutboxEvents_shouldCallPublisherService() {
        when(outboxPublisherService.publishPendingBatch()).thenReturn(2);

        OutboxPublisherScheduler scheduler = new OutboxPublisherScheduler(outboxPublisherService);

        scheduler.publishPendingOutboxEvents();

        verify(outboxPublisherService).publishPendingBatch();
    }

    @Test
    void publishPendingOutboxEvents_shouldSwallowPublisherException() {
        doThrow(new IllegalStateException("DB unavailable"))
                .when(outboxPublisherService)
                .publishPendingBatch();

        OutboxPublisherScheduler scheduler = new OutboxPublisherScheduler(outboxPublisherService);

        scheduler.publishPendingOutboxEvents();

        verify(outboxPublisherService).publishPendingBatch();
    }
}