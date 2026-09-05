package com.ecommerce.authuser.audit.web.admin;

import com.ecommerce.authuser.audit.domain.AuditTargetType;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AdminAuditLogsResponse(
        List<Item> data,
        Meta meta
) {

    public record Item(
            @JsonProperty("event_id")
            UUID eventId,

            @JsonProperty("actor_user_id")
            UUID actorUserId,

            String action,

            @JsonProperty("target_type")
            AuditTargetType targetType,

            @JsonProperty("target_id")
            UUID targetId,

            String reason,

            Map<String, Object> metadata,

            @JsonProperty("occurred_at")
            Instant occurredAt
    ) {
    }

    public record Meta(
            int page,
            int size,
            long total,

            @JsonProperty("total_pages")
            int totalPages,

            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
