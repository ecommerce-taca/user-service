package com.ecommerce.authuser.outbox.infrastructure.kafka;

import com.ecommerce.authuser.outbox.application.OutboxMessageEnvelope;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaOutboxMessageProducer {

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    public KafkaOutboxMessageProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(
            String topic,
            OutboxMessageEnvelope envelope
    ) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }

        Objects.requireNonNull(envelope, "envelope must not be null");

        try {
            String value = objectMapper.writeValueAsString(
                    envelope.toMessageBody()
            );

            ProducerRecord<String, String> record = new ProducerRecord<>(
                    topic,
                    envelope.partitionKey(),
                    value
            );

            addHeader(record, "event_id", envelope.eventId().toString());
            addHeader(record, "event_type", envelope.eventType());
            addHeader(
                    record,
                    "schema_version",
                    Short.toString(envelope.schemaVersion())
            );
            addHeader(
                    record,
                    "aggregate_type",
                    envelope.aggregateType().name()
            );
            addHeader(
                    record,
                    "aggregate_id",
                    envelope.aggregateId().toString()
            );

            kafkaTemplate
                    .send(record)
                    .get(SEND_TIMEOUT.toSeconds(), TimeUnit.SECONDS);

        } catch (JacksonException ex) {
            throw new IllegalStateException(
                    "Cannot serialize outbox message",
                    ex
            );

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Kafka publish interrupted",
                    ex
            );

        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException(
                    "Kafka publish failed",
                    ex
            );
        }
    }

    private void addHeader(
            ProducerRecord<String, String> record,
            String name,
            String value
    ) {
        record.headers().add(
            name,
            value.getBytes(StandardCharsets.UTF_8)
        );
    }
}