package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxMessageEnvelopeTest {

    @Test
    void from_shouldBuildEnvelopeFromOutboxEvent() {
        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "user.created",
                (short) 1,
                userId.toString(),
                Map.of("user_id", userId.toString())
        );

        Map<String, Object> payload = Map.of(
                "user_id", userId.toString(),
                "status", "ACTIVE"
        );

        OutboxMessageEnvelope envelope = OutboxMessageEnvelope.from(
                event,
                payload
        );

        assertThat(envelope.eventId()).isEqualTo(event.getId());
        assertThat(envelope.eventType()).isEqualTo("user.created");
        assertThat(envelope.schemaVersion()).isEqualTo((short) 1);
        assertThat(envelope.aggregateType()).isEqualTo(OutboxAggregateType.USER);
        assertThat(envelope.aggregateId()).isEqualTo(userId);
        assertThat(envelope.actorUserId()).isNull();
        assertThat(envelope.partitionKey()).isEqualTo(userId.toString());
        assertThat(envelope.payload()).containsEntry("status", "ACTIVE");
    }

    @Test
    void toMessageBody_shouldUseSnakeCaseKeys() {
        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "user.created",
                (short) 1,
                userId.toString(),
                Map.of("user_id", userId.toString())
        );

        event.getPayloadView();

        OutboxMessageEnvelope envelope = OutboxMessageEnvelope.from(
                event,
                Map.of("user_id", userId.toString())
        );

        Map<String, Object> body = envelope.toMessageBody();

        assertThat(body).containsKeys(
                "event_id",
                "event_type",
                "schema_version",
                "occurred_at",
                "aggregate_type",
                "aggregate_id",
                "actor_user_id",
                "payload"
        );

        assertThat(body).containsEntry("event_type", "user.created");
        assertThat(body).containsEntry("aggregate_type", "USER");
        assertThat(body).containsEntry("actor_user_id", null);
        assertThat(body).doesNotContainKey("partition_key");
    }

    @Test
    void from_shouldRejectNullEvent() {
        assertThatThrownBy(() -> OutboxMessageEnvelope.from(
                null,
                Map.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void from_shouldRejectNullPayload() {
        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "user.created",
                (short) 1,
                userId.toString(),
                Map.of("user_id", userId.toString())
        );

        assertThatThrownBy(() -> OutboxMessageEnvelope.from(
                event,
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toMessageBody_shouldIncludeActorUserIdWhenPresent() {
        UUID userId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.createWithActor(
                OutboxAggregateType.USER,
                userId,
                actorUserId,
                "user.status_changed",
                (short) 1,
                userId.toString(),
                Map.of("user_id", userId.toString())
        );

        OutboxMessageEnvelope envelope = OutboxMessageEnvelope.from(
                event,
                Map.of("user_id", userId.toString())
        );

        Map<String, Object> body = envelope.toMessageBody();

        assertThat(envelope.actorUserId())
                .isEqualTo(actorUserId);

        assertThat(body)
                .containsEntry(
                        "actor_user_id",
                        actorUserId.toString()
                );
    }
}