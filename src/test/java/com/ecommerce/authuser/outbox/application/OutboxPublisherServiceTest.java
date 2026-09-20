package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.outbox.infrastructure.kafka.KafkaOutboxMessageProducer;
import com.ecommerce.authuser.outbox.infrastructure.kafka.OutboxPublisherProperties;
import com.ecommerce.authuser.outbox.security.OutboxPayloadProtector;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

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
        assertThat(meterRegistry.counter(
                "auth.outbox.publish.total",
                "event_type", "user.created",
                "topic", "user.events.v1",
                "result", "success"
        ).count()).isEqualTo(1.0);
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
        assertThat(meterRegistry.counter(
                "auth.outbox.publish.total",
                "event_type", "user.created",
                "topic", "user.events.v1",
                "result", "retry"
        ).count()).isEqualTo(1.0);
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
        assertThat(meterRegistry.counter(
                "auth.outbox.publish.total",
                "event_type", "user.created",
                "topic", "user.events.v1",
                "result", "failed"
        ).count()).isEqualTo(1.0);

        assertThat(meterRegistry.counter(
                "auth.outbox.dlq.publish.total",
                "event_type", "user.created",
                "topic", "auth-user.events.dlq.v1"
        ).count()).isEqualTo(1.0);
    }

    @Test
    void publishPendingBatch_shouldPublishNotificationCommandEnvelope() {
        OutboxPublisherProperties properties = properties();

        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "AUTH_VERIFICATION_REQUESTED",
                (short) 1,
                userId.toString(),
                Map.of("protected", true)
        );

        Map<String, Object> plainPayload = Map.of(
                "command_type", "AUTH_VERIFICATION_REQUESTED",
                "user_id", userId.toString(),
                "channel", "EMAIL",
                "recipient", "minhanh@example.com",
                "template", "auth-email-verification-v1",
                "dedupe_key", "email-verification:" + userId + ":token-id",
                "data", Map.of(
                        "display_name", "Nguyen Minh Anh",
                        "verification_url", "https://taca.vn/verify?t=abc",
                        "expires_in_minutes", 1440L
                )
        );

        when(outboxEventRepository.findPendingForUpdate(
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(List.of(event));

        when(outboxPayloadProtector.unprotect(
                "AUTH_VERIFICATION_REQUESTED",
                event.getPayloadView()
        )).thenReturn(plainPayload);

        when(outboxTopicResolver.resolveTopic(event))
                .thenReturn("notification.commands.v1");

        when(outboxTopicResolver.isNotificationCommand(
                "AUTH_VERIFICATION_REQUESTED"
        )).thenReturn(true);

        OutboxPublisherService service = service(properties);

        service.publishPendingBatch();

        ArgumentCaptor<KafkaOutboxMessage> envelopeCaptor =
                ArgumentCaptor.forClass(KafkaOutboxMessage.class);

        verify(kafkaOutboxMessageProducer)
                .publish(
                        eq("notification.commands.v1"),
                        envelopeCaptor.capture()
                );

        assertThat(envelopeCaptor.getValue())
                .isInstanceOf(NotificationCommandEnvelope.class);

        Map<String, Object> messageBody =
                envelopeCaptor.getValue().toMessageBody();

        assertThat(messageBody).containsEntry(
                "command_type",
                "AUTH_VERIFICATION_REQUESTED"
        );

        assertThat(messageBody).containsEntry(
                "dedupe_key",
                "email-verification:" + userId + ":token-id"
        );

        assertThat(messageBody).doesNotContainKeys(
                "payload",
                "event_type",
                "aggregate_type",
                "aggregate_id"
        );
    }

    @Test
    void publishPendingBatch_shouldRedactDlqPayloadWhenMaxRetriesReached() {
        OutboxPublisherProperties properties = properties();
        properties.setMaxRetries(1);

        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "PHONE_OTP_REQUESTED",
                (short) 1,
                userId.toString(),
                Map.of("protected", true)
        );

        Map<String, Object> plainPayloadWithSecret = Map.of(
                "command_type", "PHONE_OTP_REQUESTED",
                "user_id", userId.toString(),
                "channel", "SMS",
                "recipient", "+84901234567",
                "template", "auth-phone-otp-v1",
                "dedupe_key", "phone-otp:challenge-id",
                "data", Map.of(
                        "challenge_id", "challenge-id",
                        "otp", "123456",
                        "expires_in_minutes", 5L
                )
        );

        when(outboxEventRepository.findPendingForUpdate(
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(List.of(event));

        when(outboxPayloadProtector.unprotect(
                "PHONE_OTP_REQUESTED",
                event.getPayloadView()
        )).thenReturn(plainPayloadWithSecret);

        when(outboxTopicResolver.resolveTopic(event))
                .thenReturn("notification.commands.v1");

        when(outboxTopicResolver.resolveDlqTopic())
                .thenReturn("auth-user.events.dlq.v1");

        when(outboxTopicResolver.isNotificationCommand(
                "PHONE_OTP_REQUESTED"
        )).thenReturn(true);

        doThrow(new IllegalStateException(
                "Kafka down otp=123456 reset_token=secret"
        )).when(kafkaOutboxMessageProducer)
                .publish(
                        eq("notification.commands.v1"),
                        any(KafkaOutboxMessage.class)
                );

        OutboxPublisherService service = service(properties);

        service.publishPendingBatch();

        ArgumentCaptor<String> topicCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<KafkaOutboxMessage> messageCaptor =
                ArgumentCaptor.forClass(KafkaOutboxMessage.class);

        verify(kafkaOutboxMessageProducer, times(2))
                .publish(
                        topicCaptor.capture(),
                        messageCaptor.capture()
                );

        assertThat(topicCaptor.getAllValues())
                .containsExactly(
                        "notification.commands.v1",
                        "auth-user.events.dlq.v1"
                );

        KafkaOutboxMessage dlqMessage =
                messageCaptor.getAllValues().get(1);

        assertThat(dlqMessage)
                .isInstanceOf(OutboxMessageEnvelope.class);

        Map<String, Object> dlqBody =
                dlqMessage.toMessageBody();

        assertThat(dlqBody)
                .containsKey("payload");

        @SuppressWarnings("unchecked")
        Map<String, Object> dlqPayload =
                (Map<String, Object>) dlqBody.get("payload");

        assertThat(dlqPayload)
                .containsEntry("original_event_type", "PHONE_OTP_REQUESTED");

        assertThat(dlqPayload)
                .containsEntry("original_payload_redacted", true);

        assertThat(dlqPayload)
                .containsEntry("failure_code", "KAFKA_PUBLISH_FAILED");

        assertThat(dlqPayload)
                .containsEntry("failure_message", "IllegalStateException");

        assertThat(dlqPayload)
                .doesNotContainKeys("original_payload");

        assertThat(dlqPayload.toString())
                .doesNotContain("123456")
                .doesNotContain("reset_token")
                .doesNotContain("secret");
    }

    private OutboxPublisherService service(OutboxPublisherProperties properties) {
        return new OutboxPublisherService(
                outboxEventRepository,
                outboxPayloadProtector,
                outboxTopicResolver,
                kafkaOutboxMessageProducer,
                properties,
                meterRegistry
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