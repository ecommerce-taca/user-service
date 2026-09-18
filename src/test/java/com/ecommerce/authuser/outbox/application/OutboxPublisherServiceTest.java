package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.outbox.infrastructure.kafka.KafkaOutboxMessageProducer;
import com.ecommerce.authuser.outbox.infrastructure.kafka.OutboxPublisherProperties;
import com.ecommerce.authuser.outbox.security.OutboxPayloadProtector;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OutboxPublisherServiceTest {

    private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    private final OutboxPayloadProtector outboxPayloadProtector = mock(OutboxPayloadProtector.class);
    private final OutboxTopicResolver outboxTopicResolver = mock(OutboxTopicResolver.class);
    private final KafkaOutboxMessageProducer kafkaOutboxMessageProducer = mock(KafkaOutboxMessageProducer.class);

    @Test
    void publishPendingBatch_shouldPublishAndMarkEventAsPublished() {
        OutboxPublisherProperties properties = properties();

        UUID userId = UUID.randomUUID();
        Map<String, Object> storedPayload = Map.of("protected", true);
        Map<String, Object> plainPayload = Map.of("user_id", userId.toString());

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "user.created",
                (short) 1,
                userId.toString(),
                storedPayload
        );

        when(outboxEventRepository.findPendingForUpdate(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(outboxPayloadProtector.unprotect("user.created", event.getPayloadView()))
                .thenReturn(plainPayload);
        when(outboxTopicResolver.resolveTopic(event))
                .thenReturn("user.events.v1");

        OutboxPublisherService service = service(properties);

        int publishedCount = service.publishPendingBatch();

        assertThat(publishedCount).isEqualTo(1);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getFailedAt()).isNull();

        ArgumentCaptor<OutboxMessageEnvelope> envelopeCaptor =
                ArgumentCaptor.forClass(OutboxMessageEnvelope.class);

        verify(kafkaOutboxMessageProducer)
                .publish(eq("user.events.v1"), envelopeCaptor.capture());

        assertThat(envelopeCaptor.getValue().eventId()).isEqualTo(event.getId());
        assertThat(envelopeCaptor.getValue().payload()).isEqualTo(plainPayload);
    }

    @Test
    void publishPendingBatch_shouldRegisterFailureWhenKafkaPublishFails() {
        OutboxPublisherProperties properties = properties();

        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "user.created",
                (short) 1,
                userId.toString(),
                Map.of("protected", true)
        );

        when(outboxEventRepository.findPendingForUpdate(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(outboxPayloadProtector.unprotect("user.created", event.getPayloadView()))
                .thenReturn(Map.of("user_id", userId.toString()));
        when(outboxTopicResolver.resolveTopic(event))
                .thenReturn("user.events.v1");

        doThrow(new IllegalStateException("Kafka down"))
                .when(kafkaOutboxMessageProducer)
                .publish(eq("user.events.v1"), any(OutboxMessageEnvelope.class));

        OutboxPublisherService service = service(properties);

        int publishedCount = service.publishPendingBatch();

        assertThat(publishedCount).isEqualTo(1);
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getFailedAt()).isNull();
        assertThat(event.getAttemptCount()).isEqualTo((byte) 1);
        assertThat(event.getNextRetryAt()).isNotNull();
        assertThat(event.getLastErrorCode()).isEqualTo("KAFKA_PUBLISH_FAILED");
    }

    @Test
    void publishPendingBatch_shouldMarkFailedWhenMaxRetriesReached() {
        OutboxPublisherProperties properties = properties();
        properties.setMaxRetries(1);

        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "user.created",
                (short) 1,
                userId.toString(),
                Map.of("protected", true)
        );

        when(outboxEventRepository.findPendingForUpdate(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(outboxPayloadProtector.unprotect("user.created", event.getPayloadView()))
                .thenReturn(Map.of("user_id", userId.toString()));
        when(outboxTopicResolver.resolveTopic(event))
                .thenReturn("user.events.v1");
        when(outboxTopicResolver.resolveDlqTopic())
                .thenReturn("auth-user.events.dlq.v1");

        doThrow(new IllegalStateException("Kafka down"))
                .when(kafkaOutboxMessageProducer)
                .publish(eq("user.events.v1"), any(OutboxMessageEnvelope.class));

        OutboxPublisherService service = service(properties);

        service.publishPendingBatch();

        assertThat(event.getAttemptCount()).isEqualTo((byte) 1);
        assertThat(event.getFailedAt()).isNotNull();
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getLastErrorCode()).isEqualTo("KAFKA_PUBLISH_FAILED");
    }

    @Test
    void publishPendingBatch_shouldSkipWhenPublisherDisabled() {
        OutboxPublisherProperties properties = properties();
        properties.setEnabled(false);

        OutboxPublisherService service = service(properties);

        int publishedCount = service.publishPendingBatch();

        assertThat(publishedCount).isZero();
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void publishPendingBatch_shouldPublishDlqWhenMaxRetriesReached() {
        OutboxPublisherProperties properties = properties();
        properties.setMaxRetries(1);

        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "user.created",
                (short) 1,
                userId.toString(),
                Map.of("protected", true)
        );

        when(outboxEventRepository.findPendingForUpdate(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(outboxPayloadProtector.unprotect("user.created", event.getPayloadView()))
                .thenReturn(Map.of("user_id", userId.toString()));
        when(outboxTopicResolver.resolveTopic(event))
                .thenReturn("user.events.v1");
        when(outboxTopicResolver.resolveDlqTopic())
                .thenReturn("auth-user.events.dlq.v1");

        doThrow(new IllegalStateException("Kafka down"))
                .when(kafkaOutboxMessageProducer)
                .publish(eq("user.events.v1"), any(OutboxMessageEnvelope.class));

        OutboxPublisherService service = service(properties);

        service.publishPendingBatch();

        verify(kafkaOutboxMessageProducer, times(1))
                .publish(eq("user.events.v1"), any(OutboxMessageEnvelope.class));

        verify(kafkaOutboxMessageProducer, times(1))
                .publish(eq("auth-user.events.dlq.v1"), any(OutboxMessageEnvelope.class));

        assertThat(event.getFailedAt()).isNotNull();
        assertThat(event.getLastErrorCode()).isEqualTo("KAFKA_PUBLISH_FAILED");
    }

    private OutboxPublisherService service(OutboxPublisherProperties properties) {
        return new OutboxPublisherService(
                outboxEventRepository,
                outboxPayloadProtector,
                outboxTopicResolver,
                kafkaOutboxMessageProducer,
                properties
        );
    }

    private OutboxPublisherProperties properties() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        properties.setEnabled(true);
        properties.setBatchSize(50);
        properties.setMaxRetries(3);
        properties.setRetryBackoffSeconds(2);
        properties.setFixedDelayMs(2000);
        return properties;
    }
}