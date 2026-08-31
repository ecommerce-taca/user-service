package com.ecommerce.authuser.shop.domain;


import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "shop_staff",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_shop_staff_member",
                        columnNames = {
                                "shop_id",
                                "user_id"
                        }
                )
        }
)
@Getter
public class ShopStaff {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "staff_role", nullable = false, length = 32)
    private String staffRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ShopStaffStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShopStaff() {
    }

    public static ShopStaff invite(
            Shop shop,
            User user,
            String staffRole,
            User invitedBy
    ) {

        Objects.requireNonNull(shop);
        Objects.requireNonNull(user);
        Objects.requireNonNull(invitedBy);

        if (staffRole == null || staffRole.isBlank()) {
            throw new IllegalArgumentException(
                    "staffRole must not be blank"
            );
        }

        Instant now = Instant.now();

        ShopStaff membership = new ShopStaff();

        membership.id = UuidV7Generator.generate();
        membership.shop = shop;
        membership.user = user;
        membership.staffRole = staffRole;
        membership.status = ShopStaffStatus.INVITED;
        membership.invitedBy = invitedBy;
        membership.invitedAt = now;

        return membership;
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

    public void activate(Instant now) {

        Objects.requireNonNull(now);

        if (status != ShopStaffStatus.INVITED) {
            throw new IllegalStateException(
                    "Only invited staff can activate"
            );
        }

        status = ShopStaffStatus.ACTIVE;

        joinedAt = now;

        revokedAt = null;
    }

    public void revoke(Instant now) {
        Objects.requireNonNull(now);

        if (status == ShopStaffStatus.REVOKED) {
            return;
        }

        status = ShopStaffStatus.REVOKED;

        revokedAt = now;
    }

    public void reinvite(User invitedBy, Instant now) {
        Objects.requireNonNull(invitedBy);
        Objects.requireNonNull(now);

        if (status != ShopStaffStatus.REVOKED) {
            throw new IllegalStateException(
                    "Only revoked staff can be reinvited"
            );
        }

        this.invitedBy = invitedBy;

        this.invitedAt = now;

        this.joinedAt = null;

        this.revokedAt = null;

        this.status = ShopStaffStatus.INVITED;
    }
}
