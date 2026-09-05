package com.ecommerce.authuser.audit.application.admin;

import com.ecommerce.authuser.audit.domain.AuditTargetType;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogQuery(
        UUID requesterUserId,

        UUID actorUserId,

        AuditTargetType targetType,

        UUID targetId,

        String action,

        Instant from,

        Instant to,

        int page,

        int size,

        SortDirection sortDirection
) {

    public enum SortDirection {
        ASC,
        DESC
    }
}
