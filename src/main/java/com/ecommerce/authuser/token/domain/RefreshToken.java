package com.ecommerce.authuser.token.domain;

import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
public class RefreshToken {

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

    @Getter(AccessLevel.NONE)
    @Column(
            name = "token_hash",
            nullable = false,
            length = 64,
            columnDefinition = "CHAR(64)"
    )
    private String tokenHash;

    @Column(name = "family_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID familyId;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoke_reason", length = 32)
    private TokenRevokeReason revokeReason;

    @Column(name = "replaced_by_token_id", columnDefinition = "BINARY(16)")
    private UUID replacedByTokenId;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshToken() {
    }

    public static RefreshToken issue(
            User user,
            String tokenHash,
            UUID familyId,
            Instant issuedAt,
            Instant expiresAt
    ) {

        Objects.requireNonNull(user);
        Objects.requireNonNull(tokenHash);
        Objects.requireNonNull(familyId);
        Objects.requireNonNull(issuedAt);
        Objects.requireNonNull(expiresAt);

        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after issuedAt"
            );
        }

        RefreshToken token = new RefreshToken();

        token.id = UuidV7Generator.generate();
        token.user = user;
        token.tokenHash = tokenHash;
        token.familyId = familyId;
        token.issuedAt = issuedAt;
        token.expiresAt = expiresAt;

        return token;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void markSeen(Instant now) {
        this.lastSeenAt = Objects.requireNonNull(now);
    }

    public void revoke(
            TokenRevokeReason reason,
            Instant now
    ) {
        Objects.requireNonNull(reason);
        Objects.requireNonNull(now);

        if (revokedAt != null) {
            return;
        }

        this.revokedAt = now;

        this.revokeReason = reason;
    }

    public void markRotated(
            UUID replacementTokenId,
            Instant now
    ) {
        Objects.requireNonNull(replacementTokenId);

        revoke(TokenRevokeReason.ROTATED, now);

        this.replacedByTokenId = replacementTokenId;
    }
}
