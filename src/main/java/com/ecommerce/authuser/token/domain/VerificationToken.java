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
@Table(name = "verification_tokens")
@Getter
public class VerificationToken {

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
    @Column(name = "channel", nullable = false, length = 8)
    private VerificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 32)
    private VerificationPurpose purpose;

    @Getter(AccessLevel.NONE)
    @Column(
            name = "token_hash",
            nullable = false,
            length = 64,
            columnDefinition = "CHAR(64)"
    )
    private String tokenHash;

    @Column(name = "recipient_masked", nullable = false, length = 254)
    private String recipientMasked;

    @Column(name = "recipient_value", length = 254)
    private String recipientValue;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "attempt_count", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private byte attemptCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VerificationToken() {
    }

    public static VerificationToken create(
            User user,
            VerificationChannel channel,
            VerificationPurpose purpose,
            String tokenHash,
            String recipientMasked,
            Instant expiresAt
    ) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(channel);
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(tokenHash);
        Objects.requireNonNull(recipientMasked);
        Objects.requireNonNull(expiresAt);

        VerificationToken token = new VerificationToken();

        token.id = UuidV7Generator.generate();
        token.user = user;
        token.channel = channel;
        token.purpose = purpose;
        token.tokenHash = tokenHash;
        token.recipientMasked = recipientMasked;
        token.expiresAt = expiresAt;
        token.attemptCount = 0;

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

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
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
                    "Verification token is not usable"
            );
        }

        this.usedAt = now;
    }

    public void revoke(Instant now) {
        Objects.requireNonNull(now);

        if (usedAt != null) {
            return;
        }

        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public void recordFailedAttempt(int maxAttempts) {
        if (attemptCount >= maxAttempts) {
            throw new IllegalStateException(
                    "Maximum attempts exceeded"
            );
        }

        attemptCount++;
    }

    public static VerificationToken createPhoneChallenge(
            User user,
            String otpHash,
            String phoneNormalized,
            String phoneMasked,
            Instant expiresAt
    ) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(otpHash);
        Objects.requireNonNull(phoneNormalized);
        Objects.requireNonNull(phoneMasked);
        Objects.requireNonNull(expiresAt);

        VerificationToken token = new VerificationToken();

        token.id = UuidV7Generator.generate();
        token.user = user;
        token.channel = VerificationChannel.PHONE;
        token.purpose = VerificationPurpose.PHONE_VERIFY;
        token.tokenHash = otpHash;
        token.recipientValue = phoneNormalized;
        token.recipientMasked = phoneMasked;
        token.expiresAt = expiresAt;
        token.attemptCount = 0;

        return token;
    }
}
