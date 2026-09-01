package com.ecommerce.authuser.mfa.domain;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "two_factor_recovery_codes")
public class TwoFactorRecoveryCode {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credential_id", nullable = false)
    private TwoFactorCredential credential;

    @Getter(AccessLevel.NONE)
    @Column(
            name = "code_hash",
            nullable = false,
            length = 64,
            columnDefinition = "CHAR(64)"
    )
    private String codeHash;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TwoFactorRecoveryCode() {
    }

    public static TwoFactorRecoveryCode create(
            TwoFactorCredential credential,
            String codeHash
    ) {
        Objects.requireNonNull(credential);

        if (codeHash == null
                || !codeHash.matches("^[0-9a-fA-F]{64}$")
        ) {
            throw new IllegalArgumentException(
                    "Invalid recovery code hash"
            );
        }

        TwoFactorRecoveryCode recoveryCode = new TwoFactorRecoveryCode();

        recoveryCode.id = UuidV7Generator.generate();
        recoveryCode.credential = credential;
        recoveryCode.codeHash = codeHash.toLowerCase();

        return recoveryCode;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed(Instant now) {
        Objects.requireNonNull(now);

        if (usedAt != null) {
            throw new IllegalStateException(
                    "Recovery code has already been used"
            );
        }

        usedAt = now;
    }
}
