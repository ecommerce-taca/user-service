package com.ecommerce.authuser.kyc.domain;

import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.Shop;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "kyc_cases")
@Getter
public class KycCase {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private KycStatus status;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_by", columnDefinition = "BINARY(16)")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "decision_reason", length = 1000)
    private String decisionReason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(
            name = "source_version",
            nullable = false,
            columnDefinition = "INT UNSIGNED"
    )
    private int sourceVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected KycCase() {
    }

    public static KycCase createDraft(
            Shop shop,
            int sourceVersion
    ) {
        Objects.requireNonNull(
                shop,
                "shop must not be null"
        );

        if (sourceVersion < 1) {
            throw new IllegalArgumentException(
                    "sourceVersion must be >= 1"
            );
        }

        KycCase kycCase = new KycCase();

        kycCase.id = UuidV7Generator.generate();
        kycCase.shop = shop;
        kycCase.status = KycStatus.DRAFT;
        kycCase.sourceVersion = sourceVersion;

        return kycCase;
    }

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }

    public void submit(Instant now) {
        Objects.requireNonNull(now);

        if (status != KycStatus.DRAFT
                && status != KycStatus.NEEDS_INFO
                && status != KycStatus.REJECTED
                && status != KycStatus.EXPIRED
        ) {
            throw new IllegalStateException(
                    "KYC case cannot be submitted from status " + status
            );
        }

        status = KycStatus.PENDING;

        submittedAt = now;

        reviewedBy = null;

        reviewedAt = null;

        decisionReason = null;
    }

    public void review(
            KycStatus decision,
            UUID reviewerUserId,
            String reason,
            Instant now
    ) {
        Objects.requireNonNull(decision);
        Objects.requireNonNull(reviewerUserId);
        Objects.requireNonNull(now);

        if (status != KycStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING KYC case can be reviewed"
            );
        }

        if (decision != KycStatus.APPROVED
                && decision != KycStatus.NEEDS_INFO
                && decision != KycStatus.REJECTED
        ) {
            throw new IllegalArgumentException(
                    "Invalid KYC review decision"
            );
        }

        String normalizedReason = normalizeDecisionReason(decision, reason);

        this.status = decision;
        this.reviewedBy = reviewerUserId;
        this.reviewedAt = now;
        this.decisionReason = normalizedReason;
    }

    private String normalizeDecisionReason(
            KycStatus decision,
            String reason
    ) {

        boolean required = decision == KycStatus.NEEDS_INFO
                || decision == KycStatus.REJECTED;

        if (reason == null) {
            if (required) {
                throw new IllegalArgumentException(
                        "Reason must contain 10-1000 Unicode characters"
                );
            }

            return null;
        }

        String normalized = reason.strip();

        int length = normalized.codePointCount(0, normalized.length());

        if (required) {
            if (length < 10 || length > 1000) {
                throw new IllegalArgumentException(
                        "Reason must contain 10-1000 Unicode characters"
                );
            }

            return normalized;
        }

        if (normalized.isEmpty()) {
            return null;
        }

        if (length > 1000) {
            throw new IllegalArgumentException(
                    "Reason must not exceed 1000 Unicode characters"
            );
        }

        return normalized;
    }
}
