package com.ecommerce.authuser.rbac.domain;

import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.common.persistence.BooleanToTinyIntConverter;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_roles_key",
                        columnNames = "role_key"
                )
        }
)
@Getter
public class Role {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID id;

    @Column(name = "role_key", nullable = false, length = 32)
    private String roleKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 16)
    private ScopeType scopeType;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Convert(converter = BooleanToTinyIntConverter.class)
    @Column(name = "is_system", nullable = false)
    private Boolean system;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Role() {
    }

    public static Role create(
            String roleKey,
            ScopeType scopeType,
            String description,
            boolean system
    ) {
        Role role = new Role();

        role.id = UuidV7Generator.generate();
        role.roleKey = roleKey;
        role.scopeType = scopeType;
        role.description = description;
        role.system = system;

        return role;
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

    public boolean isSystem() {
        return Boolean.TRUE.equals(system);
    }

}