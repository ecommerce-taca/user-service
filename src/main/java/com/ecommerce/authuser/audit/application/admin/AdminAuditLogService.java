package com.ecommerce.authuser.audit.application.admin;

import com.ecommerce.authuser.audit.domain.AuditLog;
import com.ecommerce.authuser.audit.domain.AuditTargetType;

import com.ecommerce.authuser.audit.exception.InvalidAdminAuditQueryException;

import com.ecommerce.authuser.audit.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.time.Duration;
import java.time.Instant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private static final Duration MAX_WINDOW = Duration.ofDays(31);

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminAuditAuthorizationService authorizationService;

    private final AuditLogRepository auditLogRepository;

    private final AdminAuditMetadataMasker metadataMasker;

    @Transactional(readOnly = true)
    public AdminAuditLogResult search(AdminAuditLogQuery query) {

        if (query == null || query.requesterUserId() == null) {
            throw new com.ecommerce.authuser.rbac.exception
                    .AdminRbacPermissionDeniedException();
        }

        Set<AuditTargetType> allowedTargetTypes =
                authorizationService
                        .requireTargetTypeAccess(
                                query.requesterUserId(),
                                query.targetType()
                        );

        validatePagination(query);

        String action = normalizeAction(query.action());

        TimeWindow window =
                normalizeWindow(
                        query.from(),
                        query.to(),
                        Instant.now()
                );

        AdminAuditLogQuery.SortDirection
                direction =
                query.sortDirection() == null
                        ? AdminAuditLogQuery
                          .SortDirection.DESC
                        : query.sortDirection();

        Specification<AuditLog> specification =
                buildSpecification(
                        allowedTargetTypes,
                        query,
                        action,
                        window
                );

        Sort.Direction springDirection =
                direction
                        == AdminAuditLogQuery
                        .SortDirection.ASC
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Sort sort =
                Sort.by(
                        new Sort.Order(
                                springDirection,
                                "occurredAt"
                        ),
                        new Sort.Order(
                                springDirection,
                                "id"
                        )
                );

        PageRequest pageable =
                PageRequest.of(
                        query.page() - 1,
                        query.size(),
                        sort
                );

        Page<AuditLog> page =
                auditLogRepository.findAll(
                        specification,
                        pageable
                );

        List<AdminAuditLogResult.Item> items =
                page.getContent()
                        .stream()
                        .map(this::toItem)
                        .toList();

        return new AdminAuditLogResult(
                items,
                page.getTotalElements(),
                query.page(),
                query.size(),
                page.getTotalPages()
        );
    }

    private void validatePagination(AdminAuditLogQuery query) {
        if (query.page() < 1) {
            throw new InvalidAdminAuditQueryException();
        }

        if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new InvalidAdminAuditQueryException();
        }
    }

    private String normalizeAction(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip()
                .toUpperCase(Locale.ROOT);

        int length =
                normalized.codePointCount(
                        0,
                        normalized.length()
                );

        if (length < 1 || length > 64) {
            throw new InvalidAdminAuditQueryException();
        }

        if (!AdminAuditActionCatalog.contains(normalized)) {
            throw new InvalidAdminAuditQueryException();
        }

        return normalized;
    }

    private TimeWindow normalizeWindow(
            Instant requestedFrom,
            Instant requestedTo,
            Instant now
    ) {

        Instant to = requestedTo == null ? now : requestedTo;

        Instant from = requestedFrom == null ? to.minus(MAX_WINDOW) : requestedFrom;

        if (from.isAfter(to)) {
            throw new InvalidAdminAuditQueryException();
        }

        Duration range = Duration.between(from, to);

        if (range.compareTo(MAX_WINDOW) > 0) {
            throw new InvalidAdminAuditQueryException();
        }

        return new TimeWindow(
                from,
                to
        );
    }

    private Specification<AuditLog>
    buildSpecification(
            Set<AuditTargetType> allowedTargetTypes,
            AdminAuditLogQuery query,
            String action,
            TimeWindow window
    ) {

        return (
                root,
                criteriaQuery,
                criteriaBuilder
        ) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    root.get("targetType")
                            .in(allowedTargetTypes)
            );

            if (query.actorUserId() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("actorUserId"),
                                query.actorUserId()
                        )
                );
            }

            if (query.targetId() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("targetId"),
                                query.targetId()
                        )
                );
            }

            if (action != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("action"),
                                action
                        )
                );
            }

            predicates.add(
                    criteriaBuilder
                            .greaterThanOrEqualTo(
                                    root.get("occurredAt"),
                                    window.from()
                            )
            );

            predicates.add(
                    criteriaBuilder
                            .lessThanOrEqualTo(
                                    root.get("occurredAt"),
                                    window.to()
                            )
            );

            return criteriaBuilder.and(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }

    private AdminAuditLogResult.Item toItem(AuditLog audit) {
        return new AdminAuditLogResult.Item(
                audit.getEventId(),
                audit.getActorUserId(),
                audit.getAction(),
                audit.getTargetType(),
                audit.getTargetId(),
                audit.getReason(),
                metadataMasker.mask(audit.getMetadataView()),
                audit.getOccurredAt()
        );
    }

    private record TimeWindow(
            Instant from,
            Instant to
    ) {
    }
}
