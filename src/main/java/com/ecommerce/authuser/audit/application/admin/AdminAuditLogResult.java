package com.ecommerce.authuser.audit.application.admin;

import com.ecommerce.authuser.audit.domain.AuditTargetType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AdminAuditLogResult(
        List<Item> items,
        long total,
        int page,
        int size,
        int totalPages
) {

    public record Item(
            UUID eventId,
            UUID actorUserId,
            String action,
            AuditTargetType targetType,
            UUID targetId,
            String reason,
            Map<String, Object> metadata,
            Instant occurredAt
    ) {
    }
}
