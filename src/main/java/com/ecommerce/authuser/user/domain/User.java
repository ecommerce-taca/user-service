package com.ecommerce.authuser.user.domain;

import com.ecommerce.authuser.common.id.UuidV7Generator;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
public class User {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID id;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "email_normalized", nullable = false, length = 254)
    private String emailNormalized;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "phone", length = 16)
    private String phone;

    @Column(name = "phone_normalized", length = 16)
    private String phoneNormalized;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private UserStatus status;

    @Column(name = "failed_login_count", nullable = false)
    private short failedLoginCount;

    @Column(name = "failed_login_window_started_at")
    private Instant failedLoginWindowStartedAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected User() {
    }

    public static User create(
            String email,
            String emailNormalized,
            String phone,
            String phoneNormalized,
            String passwordHash,
            String fullName,
            LocalDate dateOfBirth
    ) {

        User user = new User();

        user.id = UuidV7Generator.generate();
        user.email = email;
        user.emailNormalized = emailNormalized;
        user.phone = phone;
        user.phoneNormalized = phoneNormalized;
        user.passwordHash = passwordHash;
        user.fullName = fullName;
        user.dateOfBirth = dateOfBirth;
        user.status = UserStatus.ACTIVE;
        user.failedLoginCount = 0;

        return user;
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
}
