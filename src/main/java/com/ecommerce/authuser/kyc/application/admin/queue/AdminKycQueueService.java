package com.ecommerce.authuser.kyc.application.admin.queue;

import com.ecommerce.authuser.kyc.domain.KycCase;
import com.ecommerce.authuser.kyc.repository.KycCaseRepository;
import com.ecommerce.authuser.kyc.repository.KycDocumentRepository;

import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.Shop;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminKycQueueService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final KycCaseRepository kycCaseRepository;

    private final KycDocumentRepository kycDocumentRepository;

    @Transactional(readOnly = true)
    public AdminKycQueueResult get(AdminKycQueueQuery query) {
        AdminKycQueueQuery normalized = normalizeQuery(query);

        Sort sort =
                Sort.by(
                        normalized.ascending()
                                ? Sort.Direction.ASC
                                : Sort.Direction.DESC,
                        toEntitySortField(normalized.sortField())
                );

        PageRequest pageable =
                PageRequest.of(
                        normalized.page() - 1,
                        normalized.size(),
                        sort
                );

        String search = normalizeSearch(normalized.q());

        String taxCode = normalizeTaxCodeForSearch(search);

        Page<KycCase> result =
                kycCaseRepository.findAdminQueue(
                        normalized.status(),
                        search,
                        taxCode,
                        pageable
                );

        Map<UUID, Long> documentCounts = loadDocumentCounts(result.getContent());

        Instant now = Instant.now();

        List<AdminKycQueueItem> items =
                result
                        .getContent()
                        .stream()
                        .map(
                                kycCase ->
                                        mapItem(
                                                kycCase,
                                                documentCounts,
                                                now
                                        )
                        )
                        .toList();

        return new AdminKycQueueResult(
                items,
                normalized.page(),
                normalized.size(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private AdminKycQueueQuery normalizeQuery(AdminKycQueueQuery query) {
        if (query == null) {
            return new AdminKycQueueQuery(
                    KycStatus.PENDING,
                    DEFAULT_PAGE,
                    DEFAULT_SIZE,
                    "submitted_at",
                    true,
                    null
            );
        }

        KycStatus status =
                query.status() == null
                        ? KycStatus.PENDING
                        : query.status();

        int page = query.page() < 1
                ? DEFAULT_PAGE
                : query.page();

        int size =
                query.size() < 1
                        ? DEFAULT_SIZE
                        : Math.min(
                        query.size(),
                        MAX_SIZE
                );

        String sortField =
                query.sortField() == null || query.sortField().isBlank()
                        ? "submitted_at"
                        : query.sortField();

        return new AdminKycQueueQuery(
                status,
                page,
                size,
                sortField,
                query.ascending(),
                query.q()
        );
    }

    private String toEntitySortField(String field) {
        return switch (field) {
            case "submitted_at" -> "submittedAt";
            case "updated_at" -> "updatedAt";
            case "expires_at" -> "expiresAt";
            default ->
                    throw new IllegalArgumentException(
                            "Unsupported KYC queue sort field"
                    );
        };
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();

        if (normalized.isEmpty()) {
            return null;
        }

        int length = normalized.codePointCount(0, normalized.length());

        if (length > 120) {
            throw new IllegalArgumentException(
                    "KYC queue search exceeds 120 characters"
            );
        }

        return normalized;
    }

    private String normalizeTaxCodeForSearch(String search) {
        if (search == null) {
            return null;
        }

        String normalized = search.replaceAll("[\\s.-]", "");

        if (!normalized.matches("\\d{10,14}")) {
            return null;
        }

        return normalized;
    }

    private Map<UUID, Long> loadDocumentCounts(List<KycCase> cases) {
        if (cases.isEmpty()) {
            return Map.of();
        }

        Collection<UUID> caseIds = cases
                .stream()
                .map(KycCase::getId)
                .toList();

        return kycDocumentRepository
                .countLiveDocumentsByCaseIds(caseIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                KycDocumentRepository
                                        .CaseDocumentCount
                                        ::getKycCaseId,
                                KycDocumentRepository
                                        .CaseDocumentCount
                                        ::getDocumentCount
                        )
                );
    }

    private AdminKycQueueItem mapItem(
            KycCase kycCase,
            Map<UUID, Long> documentCounts,
            Instant now
    ) {
        Shop shop = kycCase.getShop();

        long documentCount =
                documentCounts.getOrDefault(
                        kycCase.getId(),
                        0L
                );

        return new AdminKycQueueItem(
                shop.getId(),
                shop.getName(),
                shop.getOwner().getId(),
                kycCase.getId(),
                kycCase.getStatus(),
                kycCase.getSubmittedAt(),
                documentCount,
                calculateAgeHours(
                        kycCase,
                        now
                )
        );
    }

    private double calculateAgeHours(
            KycCase kycCase,
            Instant now
    ) {

        Instant start =
                kycCase.getSubmittedAt() != null
                        ? kycCase.getSubmittedAt()
                        : kycCase.getCreatedAt();

        long seconds =
                Math.max(
                        0,
                        Duration.between(start, now).getSeconds()
                );

        return seconds / 3600.0;
    }
}
