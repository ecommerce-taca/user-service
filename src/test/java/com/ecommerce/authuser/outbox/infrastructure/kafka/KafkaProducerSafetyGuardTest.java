package com.ecommerce.authuser.outbox.infrastructure.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaProducerSafetyGuardTest {

    @Test
    void afterSingletonsInstantiated_shouldPassWhenKafkaProducerConfigIsSafe() {
        KafkaProducerSafetyGuard guard = new KafkaProducerSafetyGuard(
                safeEnvironment(),
                properties(35000)
        );

        guard.afterSingletonsInstantiated();
    }

    @Test
    void afterSingletonsInstantiated_shouldRejectNonAllAcks() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("spring.kafka.producer.acks", "1");

        KafkaProducerSafetyGuard guard = new KafkaProducerSafetyGuard(
                environment,
                properties(35000)
        );

        assertThatThrownBy(guard::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Kafka producer must use acks=all for outbox publishing");
    }

    @Test
    void afterSingletonsInstantiated_shouldRejectDisabledIdempotence() {
        MockEnvironment environment = safeEnvironment()
                .withProperty(
                        "spring.kafka.producer.properties.enable.idempotence",
                        "false"
                );

        KafkaProducerSafetyGuard guard = new KafkaProducerSafetyGuard(
                environment,
                properties(35000)
        );

        assertThatThrownBy(guard::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Kafka producer must enable idempotence for outbox publishing");
    }

    @Test
    void afterSingletonsInstantiated_shouldRejectZeroRetries() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("spring.kafka.producer.retries", "0");

        KafkaProducerSafetyGuard guard = new KafkaProducerSafetyGuard(
                environment,
                properties(35000)
        );

        assertThatThrownBy(guard::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Kafka producer retries must be >= 1 for outbox publishing");
    }

    @Test
    void afterSingletonsInstantiated_shouldRejectTooLargeMaxInFlight() {
        MockEnvironment environment = safeEnvironment()
                .withProperty(
                        "spring.kafka.producer.properties.max.in.flight.requests.per.connection",
                        "6"
                );

        KafkaProducerSafetyGuard guard = new KafkaProducerSafetyGuard(
                environment,
                properties(35000)
        );

        assertThatThrownBy(guard::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Kafka producer max.in.flight.requests.per.connection must be <= 5 when idempotence is enabled"
                );
    }

    @Test
    void afterSingletonsInstantiated_shouldRejectDeliveryTimeoutNotGreaterThanRequestTimeout() {
        MockEnvironment environment = safeEnvironment()
                .withProperty(
                        "spring.kafka.producer.properties.delivery.timeout.ms",
                        "10000"
                )
                .withProperty(
                        "spring.kafka.producer.properties.request.timeout.ms",
                        "10000"
                );

        KafkaProducerSafetyGuard guard = new KafkaProducerSafetyGuard(
                environment,
                properties(35000)
        );

        assertThatThrownBy(guard::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Kafka producer delivery.timeout.ms must be greater than request.timeout.ms"
                );
    }

    @Test
    void afterSingletonsInstantiated_shouldRejectOutboxSendTimeoutNotGreaterThanDeliveryTimeout() {
        KafkaProducerSafetyGuard guard = new KafkaProducerSafetyGuard(
                safeEnvironment(),
                properties(30000)
        );

        assertThatThrownBy(guard::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Outbox send-timeout-ms must be greater than Kafka delivery.timeout.ms"
                );
    }

    private static MockEnvironment safeEnvironment() {
        return new MockEnvironment()
                .withProperty("spring.kafka.producer.acks", "all")
                .withProperty("spring.kafka.producer.retries", "10")
                .withProperty(
                        "spring.kafka.producer.properties.enable.idempotence",
                        "true"
                )
                .withProperty(
                        "spring.kafka.producer.properties.max.in.flight.requests.per.connection",
                        "5"
                )
                .withProperty(
                        "spring.kafka.producer.properties.delivery.timeout.ms",
                        "30000"
                )
                .withProperty(
                        "spring.kafka.producer.properties.request.timeout.ms",
                        "10000"
                );
    }

    private static OutboxPublisherProperties properties(long sendTimeoutMs) {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        properties.setSendTimeoutMs(sendTimeoutMs);
        return properties;
    }
}