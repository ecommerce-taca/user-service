package com.ecommerce.authuser.support.fixture;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.support.builder.OutboxEventTestBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class OutboxFixture {

    private final OutboxEventRepository repository;

    public OutboxFixture(
            OutboxEventRepository repository
    ) {
        this.repository = repository;
    }

    public OutboxEvent userCreated(
            UUID userId
    ) {
        return save(
                OutboxEventTestBuilder
                        .anOutboxEvent()
                        .withAggregateType(OutboxAggregateType.USER)
                        .forAggregate(userId)
                        .withEventType("USER_CREATED")
                        .withPartitionKey(userId.toString())
                        .withPayload(
                                Map.of(
                                        "event",
                                        "USER_CREATED",
                                        "userId",
                                        userId.toString()
                                )
                        )
                        .build()
        );
    }

    public OutboxEvent save(
            OutboxEvent event
    ) {
        return repository.save(event);
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}
