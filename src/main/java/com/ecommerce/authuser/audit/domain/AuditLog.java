package com.ecommerce.authuser.audit.domain;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Entity
@Immutable
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(
            name = "event_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID eventId;

    @Column(name = "actor_user_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID actorUserId;

    @Column(
            name = "action",
            nullable = false,
            length = 64,
            updatable = false
    )
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "target_type",
            nullable = false,
            length = 32,
            updatable = false
    )
    private AuditTargetType targetType;

    @Column(name = "target_id", updatable = false, columnDefinition = "BINARY(16)")
    private UUID targetId;

    @Column(name = "reason", length = 1000, updatable = false)
    private String reason;

    @Getter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "metadata",
            nullable = false,
            updatable = false,
            columnDefinition = "JSON"
    )
    private Map<String, Object> metadata;

    @Column(name = "ip_hash", length = 64, updatable = false)
    private String ipHash;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditLog() {
    }

    public static AuditLog create(
            UUID actorUserId,
            String action,
            AuditTargetType targetType,
            UUID targetId,
            String reason,
            Map<String, Object> metadata,
            String ipHash,
            Instant occurredAt
    ) {

        if (action == null
                || action.isBlank()
                || action.length() > 64
        ) {
            throw new IllegalArgumentException(
                    "Invalid audit action"
            );
        }

        if (targetType == null) {
            throw new IllegalArgumentException(
                    "targetType must not be null"
            );
        }

        if (reason != null && reason.length() > 1000) {
            throw new IllegalArgumentException(
                    "Audit reason is too long"
            );
        }

        AuditLog audit = new AuditLog();

        audit.eventId = UuidV7Generator.generate();
        audit.actorUserId = actorUserId;
        audit.action = action;
        audit.targetType = targetType;
        audit.targetId = targetId;
        audit.reason = reason;
        audit.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        audit.ipHash = ipHash;
        audit.occurredAt = occurredAt != null ? occurredAt : Instant.now();

        return audit;
    }

    public Map<String, Object> getMetadataView() {
        return Map.copyOf(metadata);
    }
}
