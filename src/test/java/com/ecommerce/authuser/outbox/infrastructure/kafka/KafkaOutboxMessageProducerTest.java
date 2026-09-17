package com.ecommerce.authuser.outbox.infrastructure.kafka;

import com.ecommerce.authuser.outbox.application.OutboxMessageEnvelope;
import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaOutboxMessageProducerTest {

    @Test
    void publish_shouldSendEnvelopeWithHeaders() {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        KafkaOutboxMessageProducer producer = new KafkaOutboxMessageProducer(
                kafkaTemplate,
                new ObjectMapper()
        );

        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "user.created",
                (short) 1,
                userId.toString(),
                Map.of("user_id", userId.toString())
        );

        OutboxMessageEnvelope envelope = OutboxMessageEnvelope.from(
                event,
                Map.of(
                        "user_id", userId.toString(),
                        "status", "ACTIVE"
                )
        );

        producer.publish("user.events.v1", envelope);

        ArgumentCaptor<ProducerRecord<String, String>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);

        org.mockito.Mockito.verify(kafkaTemplate).send(captor.capture());

        ProducerRecord<String, String> record = captor.getValue();

        assertThat(record.topic()).isEqualTo("user.events.v1");
        assertThat(record.key()).isEqualTo(userId.toString());
        assertThat(record.value()).contains("\"event_type\":\"user.created\"");
        assertThat(record.value()).contains("\"aggregate_type\":\"USER\"");
        assertThat(record.value()).contains("\"status\":\"ACTIVE\"");

        assertThat(headerValue(record, "event_id"))
                .isEqualTo(event.getId().toString());
        assertThat(headerValue(record, "event_type"))
                .isEqualTo("user.created");
        assertThat(headerValue(record, "schema_version"))
                .isEqualTo("1");
        assertThat(headerValue(record, "aggregate_type"))
                .isEqualTo("USER");
        assertThat(headerValue(record, "aggregate_id"))
                .isEqualTo(userId.toString());
    }

    @Test
    void publish_shouldRejectBlankTopic() {
        KafkaOutboxMessageProducer producer = new KafkaOutboxMessageProducer(
                mock(KafkaTemplate.class),
                new ObjectMapper()
        );

        assertThatThrownBy(() -> producer.publish(" ", mockEnvelope()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publish_shouldRejectNullEnvelope() {
        KafkaOutboxMessageProducer producer = new KafkaOutboxMessageProducer(
                mock(KafkaTemplate.class),
                new ObjectMapper()
        );

        assertThatThrownBy(() -> producer.publish("user.events.v1", null))
                .isInstanceOf(NullPointerException.class);
    }

    private static String headerValue(
            ProducerRecord<String, String> record,
            String name
    ) {
        return new String(
                record.headers().lastHeader(name).value(),
                StandardCharsets.UTF_8
        );
    }

    private static OutboxMessageEnvelope mockEnvelope() {
        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "user.created",
                (short) 1,
                userId.toString(),
                Map.of("user_id", userId.toString())
        );

        return OutboxMessageEnvelope.from(
                event,
                Map.of("user_id", userId.toString())
        );
    }
}