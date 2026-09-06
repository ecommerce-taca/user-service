package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.audit.domain.AuditLog;
import com.ecommerce.authuser.audit.domain.AuditTargetType;
import com.ecommerce.authuser.support.testdata.AuditLogTestData;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class AuditLogTestBuilder {

    private UUID actorUserId;

    private String action =
            AuditLogTestData.USER_CREATED;

    private AuditTargetType targetType;

    private UUID targetId;

    private String reason;

    private Map<String, Object> metadata =
            AuditLogTestData.emptyMetadata();

    private String ipHash =
            AuditLogTestData.IP_HASH;

    private Instant occurredAt =
            AuditLogTestData.OCCURRED_AT;

    private AuditLogTestBuilder() {
    }

    public static AuditLogTestBuilder anAuditLog() {
        return new AuditLogTestBuilder();
    }

    public AuditLogTestBuilder byUser(UUID actorUserId) {
        this.actorUserId = actorUserId;
        return this;
    }

    public AuditLogTestBuilder withAction(String action) {
        this.action = action;
        return this;
    }

    public AuditLogTestBuilder withTargetType(
            AuditTargetType targetType
    ) {
        this.targetType = targetType;
        return this;
    }

    public AuditLogTestBuilder withTargetId(UUID targetId) {
        this.targetId = targetId;
        return this;
    }

    public AuditLogTestBuilder withReason(String reason) {
        this.reason = reason;
        return this;
    }

    public AuditLogTestBuilder withMetadata(
            Map<String, Object> metadata
    ) {
        this.metadata = metadata;
        return this;
    }

    public AuditLogTestBuilder withIpHash(String ipHash) {
        this.ipHash = ipHash;
        return this;
    }

    public AuditLogTestBuilder occurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
        return this;
    }

    public AuditLog build() {
        return AuditLog.create(
                actorUserId,
                action,
                targetType,
                targetId,
                reason,
                metadata,
                ipHash,
                occurredAt
        );
    }
}
