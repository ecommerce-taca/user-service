package com.ecommerce.authuser.outbox.infrastructure.kafka;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class KafkaProducerSafetyGuard implements SmartInitializingSingleton {

    private final Environment environment;

    private final OutboxPublisherProperties outboxPublisherProperties;

    public KafkaProducerSafetyGuard(
            Environment environment,
            OutboxPublisherProperties outboxPublisherProperties
    ) {
        this.environment = environment;
        this.outboxPublisherProperties = outboxPublisherProperties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        String acks = required("spring.kafka.producer.acks");
        boolean idempotenceEnabled = booleanValue(
                "spring.kafka.producer.properties.enable.idempotence"
        );
        int retries = intValue("spring.kafka.producer.retries");
        int maxInFlight = intValue(
                "spring.kafka.producer.properties.max.in.flight.requests.per.connection"
        );
        long deliveryTimeoutMs = longValue(
                "spring.kafka.producer.properties.delivery.timeout.ms"
        );
        long requestTimeoutMs = longValue(
                "spring.kafka.producer.properties.request.timeout.ms"
        );
        long sendTimeoutMs = outboxPublisherProperties.getSendTimeoutMs();

        if (!"all".equalsIgnoreCase(acks)) {
            throw new IllegalStateException(
                    "Kafka producer must use acks=all for outbox publishing"
            );
        }

        if (!idempotenceEnabled) {
            throw new IllegalStateException(
                    "Kafka producer must enable idempotence for outbox publishing"
            );
        }

        if (retries < 1) {
            throw new IllegalStateException(
                    "Kafka producer retries must be >= 1 for outbox publishing"
            );
        }

        if (maxInFlight > 5) {
            throw new IllegalStateException(
                    "Kafka producer max.in.flight.requests.per.connection must be <= 5 when idempotence is enabled"
            );
        }

        if (deliveryTimeoutMs <= requestTimeoutMs) {
            throw new IllegalStateException(
                    "Kafka producer delivery.timeout.ms must be greater than request.timeout.ms"
            );
        }

        if (sendTimeoutMs <= deliveryTimeoutMs) {
            throw new IllegalStateException(
                    "Outbox send-timeout-ms must be greater than Kafka delivery.timeout.ms"
            );
        }
    }

    private String required(String key) {
        String value = environment.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required Kafka producer property: " + key
            );
        }

        return value;
    }

    private boolean booleanValue(String key) {
        return Boolean.parseBoolean(required(key));
    }

    private int intValue(String key) {
        return Integer.parseInt(required(key));
    }

    private long longValue(String key) {
        return Long.parseLong(required(key));
    }
}