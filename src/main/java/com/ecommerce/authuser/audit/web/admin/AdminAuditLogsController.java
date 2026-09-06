package com.ecommerce.authuser.audit.web.admin;

import com.ecommerce.authuser.audit.application.admin.AdminAuditAuthorizationService;
import com.ecommerce.authuser.audit.application.admin.AdminAuditLogQuery;
import com.ecommerce.authuser.audit.application.admin.AdminAuditLogResult;
import com.ecommerce.authuser.audit.application.admin.AdminAuditLogService;

import com.ecommerce.authuser.audit.domain.AuditTargetType;

import com.ecommerce.authuser.audit.exception.InvalidAdminAuditQueryException;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.rbac.exception.AdminRbacPermissionDeniedException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogsController {

    private static final int DEFAULT_PAGE = 1;

    private static final int DEFAULT_SIZE = 20;

    private final AdminAuditLogService adminAuditLogService;

    private final AdminAuditAuthorizationService adminAuditAuthorizationService;

    @GetMapping
    public ResponseEntity<AdminAuditLogsResponse> getAuditLogs(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "actor_user_id", required = false) String actorUserId,
            @RequestParam(name = "target_type", required = false) String targetType,
            @RequestParam(name = "target_id", required = false) String targetId,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        UUID requesterUserId = parseRequesterUserId(jwt);

        adminAuditAuthorizationService
                .resolveAllowedTargetTypes(requesterUserId);

        UUID parsedActorUserId =
                parseOptionalUuid(actorUserId);

        AuditTargetType parsedTargetType =
                parseTargetType(targetType);

        UUID parsedTargetId = parseOptionalUuid(targetId);

        Instant parsedFrom = parseInstant(from);

        Instant parsedTo = parseInstant(to);

        int parsedPage = parsePage(page);

        int parsedSize = parseSize(size);

        AdminAuditLogQuery.SortDirection
                parsedSort =
                parseSort(sort);

        AdminAuditLogResult result =
                adminAuditLogService.search(
                        new AdminAuditLogQuery(
                                requesterUserId,
                                parsedActorUserId,
                                parsedTargetType,
                                parsedTargetId,
                                action,
                                parsedFrom,
                                parsedTo,
                                parsedPage,
                                parsedSize,
                                parsedSort
                        )
                );

        AdminAuditLogsResponse response =
                new AdminAuditLogsResponse(
                        result.items()
                                .stream()
                                .map(this::toResponse)
                                .toList(),

                        new AdminAuditLogsResponse.Meta(
                                result.page(),
                                result.size(),
                                result.total(),
                                result.totalPages(),
                                resolveRequestId(
                                        requestId
                                )
                        )
                );

        return ResponseEntity.ok(
                response
        );
    }

    private UUID parseRequesterUserId(Jwt jwt) {
        if (jwt == null
                || jwt.getSubject() == null
                || jwt.getSubject().isBlank()) {
            throw new AdminRbacPermissionDeniedException();
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new AdminRbacPermissionDeniedException();
        }
    }

    private UUID parseOptionalUuid(String value) {
        if (value == null) {
            return null;
        }

        if (value.isBlank()) {
            throw new InvalidAdminAuditQueryException();
        }

        try {
            return UUID.fromString(value.strip());
        } catch (IllegalArgumentException ex) {
            throw new InvalidAdminAuditQueryException();
        }
    }

    private AuditTargetType parseTargetType(String value) {
        if (value == null) {
            return null;
        }

        if (value.isBlank()) {
            throw new InvalidAdminAuditQueryException();
        }

        try {
            return AuditTargetType.valueOf(
                    value.strip()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException ex) {
            throw new InvalidAdminAuditQueryException();
        }
    }

    private Instant parseInstant(String value) {
        if (value == null) {
            return null;
        }

        if (value.isBlank()) {
            throw new InvalidAdminAuditQueryException();
        }

        try {
            return Instant.parse(value.strip());
        } catch (DateTimeParseException ex) {
            throw new InvalidAdminAuditQueryException();
        }
    }

    private int parsePage(String value) {
        if (value == null) {
            return DEFAULT_PAGE;
        }

        int parsed = parseInteger(value);

        if (parsed < 1) {
            throw new InvalidAdminAuditQueryException();
        }

        return parsed;
    }

    private int parseSize(String value) {
        if (value == null) {
            return DEFAULT_SIZE;
        }

        int parsed = parseInteger(value);

        if (parsed < 1 || parsed > 100) {
            throw new InvalidAdminAuditQueryException();
        }

        return parsed;
    }

    private int parseInteger(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidAdminAuditQueryException();
        }

        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException ex) {
            throw new InvalidAdminAuditQueryException();
        }
    }

    private AdminAuditLogQuery.SortDirection parseSort(String value) {

        if (value == null) {
            return AdminAuditLogQuery.SortDirection.DESC;
        }

        if (value.isBlank()) {
            throw new InvalidAdminAuditQueryException();
        }

        String[] parts = value.strip().split(",", -1);

        if (parts.length != 2) {
            throw new InvalidAdminAuditQueryException();
        }

        String field = parts[0].strip();

        String direction = parts[1]
                .strip()
                .toLowerCase(Locale.ROOT);

        if (!"occurred_at".equals(field)) {
            throw new InvalidAdminAuditQueryException();
        }

        return switch (direction) {

            case "asc" ->
                    AdminAuditLogQuery.SortDirection.ASC;

            case "desc" ->
                    AdminAuditLogQuery.SortDirection.DESC;

            default ->
                    throw new InvalidAdminAuditQueryException();
        };
    }

    private AdminAuditLogsResponse.Item toResponse(
            AdminAuditLogResult.Item item
    ) {

        return new AdminAuditLogsResponse.Item(
                item.eventId(),
                item.actorUserId(),
                item.action(),
                item.targetType(),
                item.targetId(),
                item.reason(),
                item.metadata(),
                item.occurredAt()
        );
    }

    private String resolveRequestId(String requestId) {

        if (requestId != null
                && !requestId.isBlank()
                && requestId.length() <= 64) {
            return requestId;
        }

        return UuidV7Generator
                .generate()
                .toString();
    }
}
