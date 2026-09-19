package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.outbox.infrastructure.kafka.KafkaOutboxMessageProducer;
import com.ecommerce.authuser.outbox.infrastructure.kafka.OutboxPublisherProperties;
import com.ecommerce.authuser.outbox.security.OutboxPayloadProtector;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class OutboxPublisherService {

    private static final String ERROR_CODE = "KAFKA_PUBLISH_FAILED";

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPayloadProtector outboxPayloadProtector;
    private final OutboxTopicResolver outboxTopicResolver;
    private final KafkaOutboxMessageProducer kafkaOutboxMessageProducer;
    private final OutboxPublisherProperties properties;

    public OutboxPublisherService(
            OutboxEventRepository outboxEventRepository,
            OutboxPayloadProtector outboxPayloadProtector,
            OutboxTopicResolver outboxTopicResolver,
            KafkaOutboxMessageProducer kafkaOutboxMessageProducer,
            OutboxPublisherProperties properties
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxPayloadProtector = outboxPayloadProtector;
        this.outboxTopicResolver = outboxTopicResolver;
        this.kafkaOutboxMessageProducer = kafkaOutboxMessageProducer;
        this.properties = properties;
    }

    @Transactional
    public int publishPendingBatch() {
        if (!properties.isEnabled()) {
            return 0;
        }

        Instant now = Instant.now();

        List<OutboxEvent> events = outboxEventRepository.findPendingForUpdate(
                now,
                PageRequest.of(0, properties.getBatchSize())
        );

        for (OutboxEvent event : events) {
            publishOne(event, now);
        }

        return events.size();
    }

    private void publishOne(OutboxEvent event, Instant now) {
        Map<String, Object> payloadForDlq = event.getPayloadView();

        try {
            Map<String, Object> payload = outboxPayloadProtector.unprotect(
                    event.getEventType(),
                    event.getPayloadView()
            );

            payloadForDlq = payload;

            KafkaOutboxMessage envelope = toKafkaMessage(event, payload);
            String topic = outboxTopicResolver.resolveTopic(event);

            kafkaOutboxMessageProducer.publish(topic, envelope);
            event.markPublished(now);

        } catch (RuntimeException ex) {
            Instant retryAt = now.plusSeconds(properties.getRetryBackoffSeconds());

            event.registerPublishFailure(
                    ERROR_CODE,
                    now,
                    retryAt,
                    properties.getMaxRetries()
            );

            if (event.getFailedAt() != null) {
                publishDlq(event, payloadForDlq, ex);
            }
        }
    }

    private void publishDlq(
            OutboxEvent event,
            Map<String, Object> payload,
            RuntimeException publishException
    ) {
        Map<String, Object> dlqPayload = Map.of(
                "original_event_type", event.getEventType(),
                "original_aggregate_type", event.getAggregateType().name(),
                "original_aggregate_id", event.getAggregateId().toString(),
                "original_partition_key", event.getPartitionKey(),
                "original_payload", payload,
                "failure_code", ERROR_CODE,
                "failure_message", safeMessage(publishException),
                "attempt_count", event.getAttemptCount()
        );

        OutboxMessageEnvelope dlqEnvelope = OutboxMessageEnvelope.from(event, dlqPayload);
        String dlqTopic = outboxTopicResolver.resolveDlqTopic();

        kafkaOutboxMessageProducer.publish(dlqTopic, dlqEnvelope);
    }

    private KafkaOutboxMessage toKafkaMessage(
            OutboxEvent event,
            Map<String, Object> payload
    ) {
        if (outboxTopicResolver.isNotificationCommand(event.getEventType())) {
            return NotificationCommandEnvelope.from(event, payload);
        }

        return OutboxMessageEnvelope.from(event, payload);
    }
    
    private String safeMessage(RuntimeException ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return ex.getClass().getSimpleName();
        }

        return ex.getMessage();
    }
}