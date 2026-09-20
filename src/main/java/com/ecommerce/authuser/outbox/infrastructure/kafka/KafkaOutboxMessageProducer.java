package com.ecommerce.authuser.outbox.infrastructure.kafka;

import com.ecommerce.authuser.outbox.application.KafkaOutboxMessage;

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

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    private final OutboxPublisherProperties properties;

    public KafkaOutboxMessageProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            OutboxPublisherProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void publish(
            String topic,
            KafkaOutboxMessage envelope
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

            envelope.kafkaHeaders()
                    .forEach((name, headerValue) -> addHeader(record, name, headerValue));

            kafkaTemplate
                    .send(record)
                    .get(properties.getSendTimeoutMs(), TimeUnit.MILLISECONDS);

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