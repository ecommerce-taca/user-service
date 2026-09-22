package com.ecommerce.authuser.outbox.infrastructure.kafka;

import com.ecommerce.authuser.outbox.application.NotificationCommandEnvelope;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class KafkaOutboxMessageProducerIntegrationTest {

    private static final String TOPIC = "notification.commands.v1";

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.1")
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publish_shouldSendNotificationCommandToKafkaWithKeyHeadersAndBody()
            throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        NotificationCommandEnvelope envelope = new NotificationCommandEnvelope(
                eventId,
                (short) 1,
                "EMAIL_VERIFICATION_REQUESTED",
                Instant.parse("2026-01-01T00:00:00Z"),
                "email-verification:" + userId,
                userId,
                "EMAIL",
                "duy@example.com",
                "auth-email-verification-v1",
                userId.toString(),
                Map.of(
                        "verification_url", "https://auth.local/verify-email?code=abc",
                        "expires_in_minutes", 15
                )
        );

        KafkaOutboxMessageProducer producer = newProducer();

        try (Consumer<String, String> consumer = newConsumer()) {
            consumer.subscribe(List.of(TOPIC));

            producer.publish(TOPIC, envelope);

            ConsumerRecord<String, String> record =
                    KafkaTestUtils.getSingleRecord(
                            consumer,
                            TOPIC,
                            Duration.ofSeconds(10)
                    );

            assertThat(record.topic()).isEqualTo(TOPIC);
            assertThat(record.key()).isEqualTo(userId.toString());

            assertThat(headerValue(record, "event_id"))
                    .isEqualTo(eventId.toString());
            assertThat(headerValue(record, "command_type"))
                    .isEqualTo("EMAIL_VERIFICATION_REQUESTED");
            assertThat(headerValue(record, "schema_version"))
                    .isEqualTo("1");

            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(
                    record.value(),
                    Map.class
            );

            assertThat(body)
                    .containsEntry("event_id", eventId.toString())
                    .containsEntry("schema_version", 1)
                    .containsEntry("command_type", "EMAIL_VERIFICATION_REQUESTED")
                    .containsEntry("occurred_at", "2026-01-01T00:00:00Z")
                    .containsEntry("dedupe_key", "email-verification:" + userId)
                    .containsEntry("user_id", userId.toString())
                    .containsEntry("channel", "EMAIL")
                    .containsEntry("recipient", "duy@example.com")
                    .containsEntry("template", "auth-email-verification-v1");

            assertThat(body)
                    .doesNotContainKeys(
                            "payload",
                            "event_type",
                            "aggregate_type",
                            "aggregate_id",
                            "partition_key"
                    );

            assertThat(record.value())
                    .doesNotContain("verification_token")
                    .doesNotContain("reset_token")
                    .doesNotContain("\"otp\"");

            @SuppressWarnings("unchecked")
            Map<String, Object> data =
                    (Map<String, Object>) body.get("data");

            assertThat(data)
                    .containsEntry(
                            "verification_url",
                            "https://auth.local/verify-email?code=abc"
                    )
                    .containsEntry("expires_in_minutes", 15);
        }
    }

    private KafkaOutboxMessageProducer newProducer() {
        Map<String, Object> producerProperties = new HashMap<>();
        producerProperties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers()
        );
        producerProperties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );
        producerProperties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProperties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000);
        producerProperties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);

        KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(producerProperties)
        );

        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        properties.setSendTimeoutMs(35000);

        return new KafkaOutboxMessageProducer(
                kafkaTemplate,
                objectMapper,
                properties
        );
    }

    private Consumer<String, String> newConsumer() {
        Map<String, Object> consumerProperties = new HashMap<>();
        consumerProperties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers()
        );
        consumerProperties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "outbox-producer-it-" + UUID.randomUUID()
        );
        consumerProperties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );
        consumerProperties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        consumerProperties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        return new KafkaConsumer<>(consumerProperties);
    }

    private static String headerValue(
            ConsumerRecord<String, String> record,
            String name
    ) {
        return new String(
                record.headers().lastHeader(name).value(),
                StandardCharsets.UTF_8
        );
    }
}