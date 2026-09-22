package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record OutboxMessageEnvelope (
        UUID eventId,
        String eventType,
        short schemaVersion,
        OutboxAggregateType aggregateType,
        UUID aggregateId,
        UUID actorUserId,
        String partitionKey,
        Instant occurredAt,
        Map<String, Object> payload
)  implements KafkaOutboxMessage {

    public static OutboxMessageEnvelope from(
            OutboxEvent event,
            Map<String, Object> payload
    ) {
        if (event == null) {
            throw new IllegalArgumentException("outbox event must not be null");
        }

        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }

        Instant occurredAt = event.getCreatedAt() == null
                ? Instant.now()
                : event.getCreatedAt();

        return new OutboxMessageEnvelope(
                event.getId(),
                event.getEventType(),
                event.getSchemaVersion(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getActorUserId(),
                event.getPartitionKey(),
                occurredAt,
                Map.copyOf(payload)
        );
    }

    @Override
    public Map<String, String> kafkaHeaders() {
        return Map.of(
                "event_id", eventId.toString(),
                "event_type", eventType,
                "schema_version", Short.toString(schemaVersion),
                "aggregate_type", aggregateType.name(),
                "aggregate_id", aggregateId.toString()
        );
    }

    @Override
    public Map<String, Object> toMessageBody() {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("event_id", eventId.toString());
        body.put("event_type", eventType);
        body.put("schema_version", schemaVersion);
        body.put("occurred_at", occurredAt.toString());
        body.put("aggregate_type", aggregateType.name());
        body.put("aggregate_id", aggregateId.toString());
        body.put(
                "actor_user_id",
                actorUserId == null ? null : actorUserId.toString()
        );
        body.put("payload", payload);

        return body;
    }
}