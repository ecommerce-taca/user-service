package com.ecommerce.authuser.audit.repository;

import com.ecommerce.authuser.audit.domain.AuditLog;
import com.ecommerce.authuser.audit.domain.AuditTargetType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Optional<AuditLog> findByEventId(UUID eventId);

    Page<AuditLog> findAllByTargetTypeAndTargetIdOrderByOccurredAtDesc(
            AuditTargetType targetType,
            UUID targetId,
            Pageable pageable
    );

    Page<AuditLog> findAllByActorUserIdOrderByOccurredAtDesc(
            UUID actorUserId,
            Pageable pageable
    );

    @Query("""
        select a
        from AuditLog a
        where a.occurredAt < :before
        order by a.occurredAt asc
        """)
    List<AuditLog> findArchiveCandidates(
            @Param("before") Instant before,
            Pageable pageable
    );
}
