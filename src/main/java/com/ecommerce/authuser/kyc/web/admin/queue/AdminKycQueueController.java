package com.ecommerce.authuser.kyc.web.admin.queue;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.kyc.application.admin.AdminKycAuthorizationService;
import com.ecommerce.authuser.kyc.application.admin.queue.AdminKycQueueItem;
import com.ecommerce.authuser.kyc.application.admin.queue.AdminKycQueueQuery;
import com.ecommerce.authuser.kyc.application.admin.queue.AdminKycQueueResult;
import com.ecommerce.authuser.kyc.application.admin.queue.AdminKycQueueService;

import com.ecommerce.authuser.kyc.exception.AdminKycPermissionDeniedException;
import com.ecommerce.authuser.kyc.exception.InvalidAdminKycQueueQueryException;

import com.ecommerce.authuser.shop.domain.KycStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/shops/kyc")
@RequiredArgsConstructor
public class AdminKycQueueController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    private final AdminKycQueueService adminKycQueueService;

    private final AdminKycAuthorizationService adminKycAuthorizationService;

    @GetMapping
    public ResponseEntity<AdminKycQueueResponse> getQueue(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "q", required = false) String q,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        UUID userId = parseUserId(jwt);

        adminKycAuthorizationService.requireKycRead(userId);

        KycStatus parsedStatus = parseStatus(status);

        int parsedPage = parsePage(page);

        int parsedSize = parseSize(size);

        ParsedSort parsedSort = parseSort(sort);

        String parsedSearch = parseSearch(q);

        AdminKycQueueResult result =
                adminKycQueueService.get(
                        new AdminKycQueueQuery(
                                parsedStatus,
                                parsedPage,
                                parsedSize,
                                parsedSort.field(),
                                parsedSort.ascending(),
                                parsedSearch
                        )
                );

        AdminKycQueueResponse response =
                new AdminKycQueueResponse(
                        result
                                .items()
                                .stream()
                                .map(this::toResponse)
                                .toList(),

                        new AdminKycQueueResponse.Meta(
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

    private UUID parseUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new AdminKycPermissionDeniedException();
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new AdminKycPermissionDeniedException();
        }
    }

    private KycStatus parseStatus(String value) {
        if (value == null) {
            return KycStatus.PENDING;
        }

        if (value.isBlank()) {
            throw new InvalidAdminKycQueueQueryException();
        }

        try {
            return KycStatus.valueOf(
                    value
                            .strip()
                            .toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException ex) {
            throw new InvalidAdminKycQueueQueryException();
        }
    }

    private int parsePage(String value) {

        if (value == null) {
            return DEFAULT_PAGE;
        }

        int parsed = parseInteger(value);

        if (parsed < 1) {
            throw new InvalidAdminKycQueueQueryException();
        }

        return parsed;
    }

    private int parseSize(String value) {

        if (value == null) {
            return DEFAULT_SIZE;
        }

        int parsed = parseInteger(value);

        if (parsed < 1 || parsed > 100) {
            throw new InvalidAdminKycQueueQueryException();
        }

        return parsed;
    }

    private int parseInteger(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidAdminKycQueueQueryException();
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new InvalidAdminKycQueueQueryException();
        }
    }

    private ParsedSort parseSort(String value) {
        if (value == null) {
            return new ParsedSort(
                    "submitted_at",
                    true
            );
        }

        if (value.isBlank()) {
            throw new InvalidAdminKycQueueQueryException();
        }

        String[] parts =
                value
                        .strip()
                        .split(
                                ",",
                                -1
                        );

        if (parts.length != 2) {
            throw new InvalidAdminKycQueueQueryException();
        }

        String field = parts[0].strip();

        String direction =
                parts[1]
                        .strip()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (!field.equals("submitted_at")
                && !field.equals("updated_at")
                && !field.equals("expires_at")) {

            throw new InvalidAdminKycQueueQueryException();
        }

        boolean ascending;

        if ("asc".equals(direction)) {
            ascending = true;

        } else if ("desc".equals(direction)) {
            ascending = false;

        } else {
            throw new InvalidAdminKycQueueQueryException();
        }

        return new ParsedSort(
                field,
                ascending
        );
    }

    private String parseSearch(String value) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.strip();

        if (normalized.isEmpty()) {
            return null;
        }

        int length = normalized.codePointCount(0, normalized.length());

        if (length > 120) {
            throw new InvalidAdminKycQueueQueryException();
        }

        return normalized;
    }

    private AdminKycQueueResponse.Item toResponse(
            AdminKycQueueItem item
    ) {

        return new AdminKycQueueResponse.Item(
                item.shopId(),
                item.shopName(),
                item.ownerUserId(),
                item.kycCaseId(),
                item.status(),
                item.submittedAt(),
                item.documentCount(),
                item.ageHours()
        );
    }

    private String resolveRequestId(
            String requestId
    ) {

        if (requestId != null
                && !requestId.isBlank()
                && requestId.length() <= 64) {

            return requestId;
        }

        return UuidV7Generator
                .generate()
                .toString();
    }

    private record ParsedSort(
            String field,
            boolean ascending
    ) {
    }
}
