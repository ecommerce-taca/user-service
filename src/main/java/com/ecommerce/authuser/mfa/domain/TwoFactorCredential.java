package com.ecommerce.authuser.mfa.domain;

import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.user.domain.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "two_factor_credentials",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_two_factor_user",
                        columnNames = "user_id"
                )
        }
)
@Getter
public class TwoFactorCredential {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Getter(AccessLevel.NONE)
    @Column(
            name = "secret_ciphertext",
            nullable = false,
            columnDefinition = "VARBINARY(1024)"
    )
    private byte[] secretCiphertext;

    @Column(name = "key_version", nullable = false, length = 32)
    private String keyVersion;

    @Column(name = "last_totp_step")
    private Long lastTotpStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TwoFactorStatus status;

    @Column(name = "enabled_at")
    private Instant enabledAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TwoFactorCredential() {
    }

    public static TwoFactorCredential createEnrollment(
            User user,
            byte[] secretCiphertext,
            String keyVersion
    ) {
        Objects.requireNonNull(user);

        validateSecret(secretCiphertext, keyVersion);

        TwoFactorCredential credential = new TwoFactorCredential();

        credential.id = UuidV7Generator.generate();
        credential.user = user;
        credential.secretCiphertext = Arrays.copyOf(secretCiphertext, secretCiphertext.length);
        credential.keyVersion = keyVersion;
        credential.status = TwoFactorStatus.ENROLLING;

        return credential;
    }

    private static void validateSecret(
            byte[] ciphertext,
            String keyVersion
    ) {
        if (ciphertext == null
                || ciphertext.length == 0
                || ciphertext.length > 1024
        ) {
            throw new IllegalArgumentException(
                    "Invalid encrypted TOTP secret"
            );
        }

        if (keyVersion == null
                || keyVersion.isBlank()
                || keyVersion.length() > 32
        ) {
            throw new IllegalArgumentException(
                    "Invalid key version"
            );
        }
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

    public void beginEnrollment(
            byte[] newSecretCiphertext,
            String newKeyVersion
    ) {

        if (status == TwoFactorStatus.ENABLED) {
            throw new IllegalStateException(
                    "Enabled credential cannot be re-enrolled directly"
            );
        }

        validateSecret(newSecretCiphertext, newKeyVersion);

        this.secretCiphertext = Arrays.copyOf(newSecretCiphertext, newSecretCiphertext.length);
        this.keyVersion = newKeyVersion;
        this.status = TwoFactorStatus.ENROLLING;
        this.enabledAt = null;
        this.disabledAt = null;
        this.lastTotpStep = null;
    }

    public void enable(Instant now) {
        Objects.requireNonNull(now);

        if (status != TwoFactorStatus.ENROLLING
                && status != TwoFactorStatus.RESET_REQUIRED) {
            throw new IllegalStateException(
                    "Only ENROLLING or RESET_REQUIRED credential can be enabled"
            );
        }

        status = TwoFactorStatus.ENABLED;

        enabledAt = now;

        disabledAt = null;
    }

    public void disable(Instant now) {
        Objects.requireNonNull(now);

        if (status == TwoFactorStatus.DISABLED) {
            return;
        }

        status = TwoFactorStatus.DISABLED;

        disabledAt = now;
    }

    public void requireReset() {
        if (status != TwoFactorStatus.ENABLED) {
            throw new IllegalStateException(
                    "Only ENABLED credential can require reset"
            );
        }

        status = TwoFactorStatus.RESET_REQUIRED;
    }

    public byte[] getSecretCiphertextCopy() {
        return Arrays.copyOf(
                secretCiphertext,
                secretCiphertext.length
        );
    }

    public boolean hasUsedTotpStep(long step) {
        if (step < 0) {
            throw new IllegalArgumentException(
                    "TOTP step must not be negative"
            );
        }

        return lastTotpStep != null && step <= lastTotpStep;
    }

    public void recordTotpStepUsed(long step) {
        if (step < 0) {
            throw new IllegalArgumentException(
                    "TOTP step must not be negative"
            );
        }

        if (lastTotpStep != null && step <= lastTotpStep) {

            throw new IllegalStateException(
                    "TOTP step has already been used"
            );
        }

        lastTotpStep = step;
    }

    public void beginResetEnrollment(
            byte[] newSecretCiphertext,
            String newKeyVersion
    ) {

        if (status != TwoFactorStatus.RESET_REQUIRED) {
            throw new IllegalStateException(
                    "Only RESET_REQUIRED credential can begin reset enrollment"
            );
        }

        validateSecret(newSecretCiphertext, newKeyVersion);

        this.secretCiphertext = Arrays.copyOf(
                newSecretCiphertext,
                newSecretCiphertext.length
        );

        this.keyVersion = newKeyVersion;

        this.status = TwoFactorStatus.RESET_REQUIRED;

        this.enabledAt = null;
        this.disabledAt = null;

        this.lastTotpStep = null;
    }
}
