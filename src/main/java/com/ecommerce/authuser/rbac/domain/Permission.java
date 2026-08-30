package com.ecommerce.authuser.rbac.domain;

import com.ecommerce.authuser.common.id.UuidV7Generator;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_permissions_key",
                        columnNames = "permission_key"
                )
        }
)
public class Permission {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID id;

    @Column(name = "permission_key", nullable = false, length = 64)
    private String permissionKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 16)
    private ScopeType scopeType;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Permission() {
    }

    public static Permission create(
            String permissionKey,
            ScopeType scopeType,
            String description
    ) {

        Permission permission = new Permission();

        permission.id = UuidV7Generator.generate();
        permission.permissionKey = permissionKey;
        permission.scopeType = scopeType;
        permission.description = description;

        return permission;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getPermissionKey() {
        return permissionKey;
    }

    public ScopeType getScopeType() {
        return scopeType;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
