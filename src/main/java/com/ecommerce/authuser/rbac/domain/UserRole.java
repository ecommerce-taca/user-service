package com.ecommerce.authuser.rbac.domain;

import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_roles")
@Getter
public class UserRole {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @Column(name = "granted_by", columnDefinition = "BINARY(16)")
    private UUID grantedBy;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserRole() {
    }

    public static UserRole assign(
            User user,
            Role role,
            Shop shop,
            UUID grantedBy
    ) {

        Objects.requireNonNull(user, "user must not be null");

        Objects.requireNonNull(role, "role must not be null");

        validateScope(role, shop);

        UserRole assignment = new UserRole();

        assignment.id = UuidV7Generator.generate();
        assignment.user = user;
        assignment.role = role;
        assignment.shop = shop;
        assignment.grantedBy = grantedBy;

        return assignment;
    }

    private static void validateScope(Role role, Shop shop) {
        if (role.getScopeType() == ScopeType.SHOP && shop == null) {
            throw new IllegalArgumentException(
                    "SHOP scoped role requires shop"
            );
        }

        if (role.getScopeType() != ScopeType.SHOP && shop != null) {
            throw new IllegalArgumentException(
                    "Non-SHOP role must not have shop"
            );
        }
    }

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();

        if (grantedAt == null) {
            grantedAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isActive() {
        return revokedAt == null;
    }

    public void revoke(Instant now) {
        Objects.requireNonNull(now);

        if (revokedAt != null) {
            return;
        }

        revokedAt = now;
    }

    public void reactivate(
            UUID grantedBy,
            Instant now
    ) {
        Objects.requireNonNull(now);

        this.grantedBy = grantedBy;

        this.grantedAt = now;

        this.revokedAt = null;
    }
}
