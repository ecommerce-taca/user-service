package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.support.testdata.OutboxTestData;

import java.util.Map;
import java.util.UUID;

public final class OutboxEventTestBuilder {

    private OutboxAggregateType aggregateType;

    private UUID aggregateId;

    private String eventType =
            OutboxTestData.USER_CREATED;

    private short schemaVersion =
            OutboxTestData.SCHEMA_VERSION;

    private String partitionKey;

    private Map<String, Object> payload;

    private OutboxEventTestBuilder() {
    }

    public static OutboxEventTestBuilder anOutboxEvent() {
        return new OutboxEventTestBuilder();
    }

    public OutboxEventTestBuilder withAggregateType(
            OutboxAggregateType aggregateType
    ) {
        this.aggregateType = aggregateType;
        return this;
    }

    public OutboxEventTestBuilder forAggregate(
            UUID aggregateId
    ) {
        this.aggregateId = aggregateId;
        return this;
    }

    public OutboxEventTestBuilder withEventType(
            String eventType
    ) {
        this.eventType = eventType;
        return this;
    }

    public OutboxEventTestBuilder withSchemaVersion(
            short schemaVersion
    ) {
        this.schemaVersion = schemaVersion;
        return this;
    }

    public OutboxEventTestBuilder withPartitionKey(
            String partitionKey
    ) {
        this.partitionKey = partitionKey;
        return this;
    }

    public OutboxEventTestBuilder withPayload(
            Map<String, Object> payload
    ) {
        this.payload = payload;
        return this;
    }

    public OutboxEvent build() {
        return OutboxEvent.create(
                aggregateType,
                aggregateId,
                eventType,
                schemaVersion,
                partitionKey,
                payload
        );
    }
}
