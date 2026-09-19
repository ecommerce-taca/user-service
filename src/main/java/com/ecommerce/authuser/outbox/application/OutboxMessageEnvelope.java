package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OutboxMessageEnvelope (
        UUID eventId,
        String eventType,
        short schemaVersion,
        OutboxAggregateType aggregateType,
        UUID aggregateId,
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

    public Map<String, Object> toMessageBody() {
        return Map.of(
            "event_id", eventId.toString(),
            "event_type", eventType,
            "schema_version", schemaVersion,
            "aggregate_type", aggregateType.name(),
            "aggregate_id", aggregateId.toString(),
            "partition_key", partitionKey,
            "occurred_at", occurredAt.toString(),
            "payload", payload
        );
    }
}