package com.ecommerce.authuser.audit.repository;

import com.ecommerce.authuser.audit.domain.AuditLog;
import com.ecommerce.authuser.audit.domain.AuditTargetType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog auditLog);

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
}
