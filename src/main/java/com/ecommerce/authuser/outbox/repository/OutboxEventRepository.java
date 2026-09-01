package com.ecommerce.authuser.outbox.repository;

import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select event
        from OutboxEvent event
        where event.publishedAt is null
          and event.failedAt is null
          and (
              event.nextRetryAt is null
              or event.nextRetryAt <= :now
          )
        order by event.createdAt asc
        """)
    List<OutboxEvent> findPendingForUpdate(
            @Param("now") Instant now,
            Pageable pageable
    );

    List<OutboxEvent> findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
            OutboxAggregateType aggregateType,
            UUID aggregateId
    );
}
