package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.outbox.infrastructure.kafka.KafkaOutboxMessageProducer;
import com.ecommerce.authuser.outbox.infrastructure.kafka.OutboxPublisherProperties;
import com.ecommerce.authuser.outbox.security.OutboxPayloadProtector;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);

    private static final String PUBLISH_TOTAL_METRIC = "auth.outbox.publish.total";
    private static final String PUBLISH_DURATION_METRIC = "auth.outbox.publish.duration";
    private static final String DLQ_PUBLISH_TOTAL_METRIC = "auth.outbox.dlq.publish.total";

    private final MeterRegistry meterRegistry;

    public OutboxPublisherService(
            OutboxEventRepository outboxEventRepository,
            OutboxPayloadProtector outboxPayloadProtector,
            OutboxTopicResolver outboxTopicResolver,
            KafkaOutboxMessageProducer kafkaOutboxMessageProducer,
            OutboxPublisherProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxPayloadProtector = outboxPayloadProtector;
        this.outboxTopicResolver = outboxTopicResolver;
        this.kafkaOutboxMessageProducer = kafkaOutboxMessageProducer;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
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
        Timer.Sample timer = Timer.start(meterRegistry);
        String topic = "unresolved";

        try {
            Map<String, Object> payload = outboxPayloadProtector.unprotect(
                    event.getEventType(),
                    event.getPayloadView()
            );

            payloadForDlq = payload;

            KafkaOutboxMessage envelope = toKafkaMessage(event, payload);
            topic = outboxTopicResolver.resolveTopic(event);

            kafkaOutboxMessageProducer.publish(topic, envelope);
            event.markPublished(now);

            recordPublishMetrics(
                    timer,
                    event.getEventType(),
                    topic,
                    "success"
            );

            log.info(
                    "Published outbox event event_id={} event_type={} topic={} aggregate_type={} aggregate_id={} attempt_count={}",
                    event.getId(),
                    event.getEventType(),
                    topic,
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getAttemptCount()
            );

        } catch (RuntimeException ex) {
            Instant retryAt = now.plusSeconds(properties.getRetryBackoffSeconds());

            event.registerPublishFailure(
                    ERROR_CODE,
                    now,
                    retryAt,
                    properties.getMaxRetries()
            );

            String result = event.getFailedAt() == null ? "retry" : "failed";

            recordPublishMetrics(
                    timer,
                    event.getEventType(),
                    topic,
                    result
            );

            log.warn(
                    "Outbox publish failed event_id={} event_type={} topic={} aggregate_type={} aggregate_id={} attempt_count={} result={} error_code={} exception={}",
                    event.getId(),
                    event.getEventType(),
                    topic,
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getAttemptCount(),
                    result,
                    ERROR_CODE,
                    ex.getClass().getSimpleName()
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
                "original_payload_redacted", true,
                "failure_code", ERROR_CODE,
                "failure_message", safeMessage(publishException),
                "attempt_count", event.getAttemptCount()
        );

        OutboxMessageEnvelope dlqEnvelope = OutboxMessageEnvelope.from(event, dlqPayload);
        String dlqTopic = outboxTopicResolver.resolveDlqTopic();

        kafkaOutboxMessageProducer.publish(dlqTopic, dlqEnvelope);
        meterRegistry.counter(
                DLQ_PUBLISH_TOTAL_METRIC,
                "event_type", event.getEventType(),
                "topic", dlqTopic
        ).increment();

        log.warn(
                "Published outbox DLQ event_id={} original_event_type={} dlq_topic={} aggregate_type={} aggregate_id={} attempt_count={}",
                event.getId(),
                event.getEventType(),
                dlqTopic,
                event.getAggregateType(),
                event.getAggregateId(),
                event.getAttemptCount()
        );
    }

    private void recordPublishMetrics(
            Timer.Sample timer,
            String eventType,
            String topic,
            String result
    ) {
        Tags tags = Tags.of(
                "event_type", eventType,
                "topic", topic,
                "result", result
        );

        meterRegistry.counter(PUBLISH_TOTAL_METRIC, tags)
                .increment();

        timer.stop(
                Timer.builder(PUBLISH_DURATION_METRIC)
                        .tags(tags)
                        .register(meterRegistry)
        );
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
        return ex.getClass().getSimpleName();
    }
}