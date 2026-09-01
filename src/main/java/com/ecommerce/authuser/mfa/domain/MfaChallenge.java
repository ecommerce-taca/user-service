package com.ecommerce.authuser.mfa.domain;

import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.user.domain.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "mfa_challenges")
public class MfaChallenge {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 16)
    private MfaPurpose purpose;

    @Getter(AccessLevel.NONE)
    @Column(name = "code_hash", length = 64, columnDefinition = "CHAR(64)")
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempt_count", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private byte attemptCount;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MfaChallenge() {
    }

    public static MfaChallenge create(
            User user,
            MfaPurpose purpose,
            String codeHash,
            Instant createdAt,
            Instant expiresAt
    ) {

        Objects.requireNonNull(user);
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(expiresAt);

        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after createdAt"
            );
        }

        if (codeHash != null
                && !codeHash.matches("^[0-9a-fA-F]{64}$")
        ) {
            throw new IllegalArgumentException(
                    "Invalid MFA code hash"
            );
        }

        MfaChallenge challenge = new MfaChallenge();

        challenge.id = UuidV7Generator.generate();
        challenge.user = user;
        challenge.purpose = purpose;
        challenge.codeHash = codeHash == null ? null : codeHash.toLowerCase();
        challenge.createdAt = createdAt;
        challenge.expiresAt = expiresAt;
        challenge.attemptCount = 0;

        return challenge;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isUsable(
            Instant now,
            int maxAttempts
    ) {

        return verifiedAt == null
                && revokedAt == null
                && expiresAt.isAfter(now)
                && attemptCount < maxAttempts;
    }

    public void recordFailedAttempt(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "maxAttempts must be positive"
            );
        }

        if (attemptCount >= maxAttempts) {
            throw new IllegalStateException(
                    "Maximum MFA attempts exceeded"
            );
        }

        attemptCount++;
    }

    public void markVerified(Instant now, int maxAttempts) {
        Objects.requireNonNull(now);

        if (!isUsable(now, maxAttempts)) {
            throw new IllegalStateException(
                    "MFA challenge is not usable"
            );
        }

        verifiedAt = now;
    }

    public void revoke(Instant now) {
        Objects.requireNonNull(now);

        if (verifiedAt != null) {
            return;
        }

        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}
