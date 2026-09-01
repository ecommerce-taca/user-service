package com.ecommerce.authuser.outbox.domain;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "aggregate_type",
            nullable = false,
            length = 32,
            updatable = false
    )
    private OutboxAggregateType aggregateType;

    @Column(
            name = "aggregate_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID aggregateId;

    @Column(
            name = "event_type",
            nullable = false,
            length = 64,
            updatable = false
    )
    private String eventType;

    @Column(
            name = "schema_version",
            nullable = false,
            updatable = false,
            columnDefinition = "SMALLINT UNSIGNED"
    )
    private short schemaVersion;

    @Column(
            name = "partition_key",
            nullable = false,
            length = 128,
            updatable = false
    )
    private String partitionKey;

    @Getter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "payload",
            nullable = false,
            updatable = false,
            columnDefinition = "JSON"
    )
    private Map<String, Object> payload;

    @Column(
            name = "attempt_count",
            nullable = false,
            columnDefinition = "TINYINT UNSIGNED"
    )
    private byte attemptCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "last_error_code", length = 64)
    private String lastErrorCode;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    protected OutboxEvent() {
    }

    public static OutboxEvent create(
            OutboxAggregateType aggregateType,
            UUID aggregateId,
            String eventType,
            short schemaVersion,
            String partitionKey,
            Map<String, Object> payload
    ) {
        if (aggregateType == null) {
            throw new IllegalArgumentException(
                    "aggregateType must not be null"
            );
        }

        if (aggregateId == null) {
            throw new IllegalArgumentException(
                    "aggregateId must not be null"
            );
        }

        if (eventType == null
                || eventType.isBlank()
                || eventType.length() > 64
        ) {
            throw new IllegalArgumentException(
                    "Invalid eventType"
            );
        }

        if (schemaVersion < 1) {
            throw new IllegalArgumentException(
                    "schemaVersion must be >= 1"
            );
        }

        if (partitionKey == null
                || partitionKey.isBlank()
                || partitionKey.length() > 128
        ) {
            throw new IllegalArgumentException(
                    "Invalid partitionKey"
            );
        }

        if (payload == null) {
            throw new IllegalArgumentException(
                    "payload must not be null"
            );
        }

        OutboxEvent event = new OutboxEvent();

        event.id = UuidV7Generator.generate();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.schemaVersion = schemaVersion;
        event.partitionKey = partitionKey;
        event.payload = Map.copyOf(payload);
        event.attemptCount = 0;

        return event;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Map<String, Object> getPayloadView() {
        return Map.copyOf(payload);
    }

    public void markPublished(Instant now) {
        if (publishedAt != null) {
            return;
        }

        if (failedAt != null) {
            throw new IllegalStateException(
                    "Failed outbox event cannot be published"
            );
        }

        publishedAt = now;

        nextRetryAt = null;

        lastErrorCode = null;
    }

    public void registerPublishFailure(
            String errorCode,
            Instant now,
            Instant retryAt,
            int maxRetries
    ) {

        if (publishedAt != null) {
            throw new IllegalStateException(
                    "Published event cannot fail"
            );
        }

        if (failedAt != null) {
            throw new IllegalStateException(
                    "Event is already terminally failed"
            );
        }

        if (maxRetries < 1 || maxRetries > 3) {
            throw new IllegalArgumentException(
                    "maxRetries must be between 1 and 3"
            );
        }

        if (errorCode == null
                || errorCode.isBlank()
                || errorCode.length() > 64
        ) {
            throw new IllegalArgumentException(
                    "Invalid error code"
            );
        }

        lastErrorCode = errorCode;

        if (attemptCount >= maxRetries) {
            failedAt = now;

            nextRetryAt = null;

            return;
        }

        attemptCount++;

        nextRetryAt = retryAt;
    }
}
