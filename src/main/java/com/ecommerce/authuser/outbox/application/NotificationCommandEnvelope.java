package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.domain.OutboxEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationCommandEnvelope(
        UUID eventId,
        short schemaVersion,
        String commandType,
        Instant occurredAt,
        String dedupeKey,
        UUID userId,
        String channel,
        String recipient,
        String template,
        String partitionKey,
        Map<String, Object> data
) implements KafkaOutboxMessage {

    public static NotificationCommandEnvelope from(
            OutboxEvent event,
            Map<String, Object> payload
    ) {
        if (event == null) {
            throw new IllegalArgumentException("outbox event must not be null");
        }

        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }

        String commandType = requiredString(payload, "command_type");
        String userId = requiredString(payload, "user_id");
        String channel = requiredString(payload, "channel");
        String recipient = requiredString(payload, "recipient");
        String template = requiredString(payload, "template");
        String dedupeKey = requiredString(payload, "dedupe_key");
        Map<String, Object> data = requiredMap(payload, "data");

        Instant occurredAt = event.getCreatedAt() == null
                ? Instant.now()
                : event.getCreatedAt();

        return new NotificationCommandEnvelope(
                event.getId(),
                event.getSchemaVersion(),
                commandType,
                occurredAt,
                dedupeKey,
                UUID.fromString(userId),
                channel,
                recipient,
                template,
                event.getPartitionKey(),
                Map.copyOf(data)
        );
    }

    @Override
    public Map<String, String> kafkaHeaders() {
        return Map.of(
                "event_id", eventId.toString(),
                "command_type", commandType,
                "schema_version", Short.toString(schemaVersion)
        );
    }

    @Override
    public Map<String, Object> toMessageBody() {
        return Map.of(
                "event_id", eventId.toString(),
                "schema_version", schemaVersion,
                "command_type", commandType,
                "occurred_at", occurredAt.toString(),
                "dedupe_key", dedupeKey,
                "user_id", userId.toString(),
                "channel", channel,
                "recipient", recipient,
                "template", template,
                "data", data
        );
    }

    private static String requiredString(
            Map<String, Object> payload,
            String key
    ) {
        Object value = payload.get(key);

        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(
                    "notification command requires " + key
            );
        }

        return text;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requiredMap(
            Map<String, Object> payload,
            String key
    ) {
        Object value = payload.get(key);

        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(
                    "notification command requires " + key
            );
        }

        return (Map<String, Object>) map;
    }
}