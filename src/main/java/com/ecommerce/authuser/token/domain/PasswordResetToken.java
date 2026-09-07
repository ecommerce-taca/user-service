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
@Table(name = "password_reset_tokens")
@Getter
public class PasswordResetToken {

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

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PasswordResetToken() {
    }

    public static PasswordResetToken create(
            User user,
            String tokenHash,
            Instant expiresAt
    ) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(tokenHash);
        Objects.requireNonNull(expiresAt);

        PasswordResetToken token = new PasswordResetToken();

        token.id = UuidV7Generator.generate();
        token.user = user;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;

        return token;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (expiresAt != null && !expiresAt.isAfter(createdAt)) {
            throw new IllegalStateException(
                    "expiresAt must be after createdAt"
            );
        }
    }

    public boolean isUsable(Instant now) {
        return usedAt == null
                && revokedAt == null
                && expiresAt.isAfter(now);
    }

    public void markUsed(Instant now) {
        Objects.requireNonNull(now);

        if (!isUsable(now)) {
            throw new IllegalStateException(
                    "Password reset token is not usable"
            );
        }

        usedAt = now;
    }

    public void revoke(
            Instant now
    ) {

        Objects.requireNonNull(now);

        if (usedAt == null && revokedAt == null) {
            revokedAt = now;
        }
    }
}
