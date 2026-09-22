package com.ecommerce.authuser.outbox.repository;

import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    @Query("""
        select count(event)
        from OutboxEvent event
        where event.publishedAt is null
          and event.failedAt is null
          and (
              event.nextRetryAt is null
              or event.nextRetryAt <= :now
          )
        """)
    long countReadyToPublish(@Param("now") Instant now);

    @Query("""
        select count(event)
        from OutboxEvent event
        where event.publishedAt is null
          and event.failedAt is null
          and event.nextRetryAt > :now
        """)
    long countWaitingForRetry(@Param("now") Instant now);

    @Query("""
        select count(event)
        from OutboxEvent event
        where event.failedAt is not null
        """)
    long countFailed();

    @Query("""
        select min(event.createdAt)
        from OutboxEvent event
        where event.publishedAt is null
          and event.failedAt is null
          and (
              event.nextRetryAt is null
              or event.nextRetryAt <= :now
          )
        """)
    Optional<Instant> findOldestReadyToPublishCreatedAt(
            @Param("now") Instant now
    );

    @Query("""
        select event.id
        from OutboxEvent event
        where event.publishedAt is not null
          and event.publishedAt < :cutoff
        order by event.publishedAt asc
        """)
    List<UUID> findPublishedIdsForCleanup(
            @Param("cutoff") Instant cutoff,
            Pageable pageable
    );

    @Query("""
        select event.id
        from OutboxEvent event
        where event.failedAt is not null
          and event.failedAt < :cutoff
        order by event.failedAt asc
        """)
    List<UUID> findFailedIdsForCleanup(
            @Param("cutoff") Instant cutoff,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from OutboxEvent event
        where event.id in :ids
        """)
    int deleteAllByIdIn(@Param("ids") List<UUID> ids);
}
