package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationCommandEnvelopeTest {

    @Test
    void from_shouldBuildNotificationCommandContract() {
        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "AUTH_VERIFICATION_REQUESTED",
                (short) 1,
                userId.toString(),
                Map.of("protected", true)
        );

        NotificationCommandEnvelope envelope =
                NotificationCommandEnvelope.from(
                        event,
                        Map.of(
                                "command_type", "AUTH_VERIFICATION_REQUESTED",
                                "user_id", userId.toString(),
                                "channel", "EMAIL",
                                "recipient", "minhanh@example.com",
                                "template", "auth-email-verification-v1",
                                "dedupe_key", "email-verification:"
                                        + userId
                                        + ":token-id",
                                "data", Map.of(
                                        "display_name", "Nguyen Minh Anh",
                                        "verification_url", "https://taca.vn/verify?t=abc",
                                        "expires_in_minutes", 30L
                                )
                        )
                );

        Map<String, Object> body = envelope.toMessageBody();

        assertThat(body).containsKeys(
                "event_id",
                "schema_version",
                "command_type",
                "occurred_at",
                "dedupe_key",
                "user_id",
                "channel",
                "recipient",
                "template",
                "data"
        );

        assertThat(body).doesNotContainKeys(
                "payload",
                "event_type",
                "aggregate_type",
                "aggregate_id",
                "partition_key"
        );

        assertThat(body).containsEntry(
                "command_type",
                "AUTH_VERIFICATION_REQUESTED"
        );

        assertThat(body).containsEntry(
                "dedupe_key",
                "email-verification:" + userId + ":token-id"
        );

        assertThat(body.get("data"))
                .isInstanceOf(Map.class);
    }

    @Test
    void kafkaHeaders_shouldUseNotificationHeadersOnly() {
        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "AUTH_VERIFICATION_REQUESTED",
                (short) 1,
                userId.toString(),
                Map.of("protected", true)
        );

        NotificationCommandEnvelope envelope =
                NotificationCommandEnvelope.from(
                        event,
                        Map.of(
                                "command_type", "AUTH_VERIFICATION_REQUESTED",
                                "user_id", userId.toString(),
                                "channel", "EMAIL",
                                "recipient", "minhanh@example.com",
                                "template", "auth-email-verification-v1",
                                "dedupe_key", "email-verification:"
                                        + userId
                                        + ":token-id",
                                "data", Map.of(
                                        "verification_url", "https://taca.vn/verify?t=abc",
                                        "expires_in_minutes", 30L
                                )
                        )
                );

        Map<String, String> headers = envelope.kafkaHeaders();

        assertThat(headers).containsEntry(
                "event_id",
                event.getId().toString()
        );

        assertThat(headers).containsEntry(
                "command_type",
                "AUTH_VERIFICATION_REQUESTED"
        );

        assertThat(headers).containsEntry(
                "schema_version",
                "1"
        );

        assertThat(headers).doesNotContainKeys(
                "event_type",
                "aggregate_type",
                "aggregate_id"
        );
    }

    @Test
    void from_shouldRejectMissingRequiredData() {
        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "AUTH_VERIFICATION_REQUESTED",
                (short) 1,
                userId.toString(),
                Map.of("protected", true)
        );

        assertThatThrownBy(() -> NotificationCommandEnvelope.from(
                event,
                Map.of(
                        "command_type", "AUTH_VERIFICATION_REQUESTED",
                        "user_id", userId.toString(),
                        "channel", "EMAIL",
                        "recipient", "minhanh@example.com",
                        "template", "auth-email-verification-v1",
                        "dedupe_key", "email-verification:"
                                + userId
                                + ":token-id"
                )
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
