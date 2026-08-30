package com.ecommerce.authuser.rbac.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "role_permissions")
public class RolePermission {

    @EmbeddedId
    private RolePermissionId id;

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @MapsId("permissionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    @Column(name = "granted_by", columnDefinition = "BINARY(16)")
    private UUID grantedBy;

    protected RolePermission() {
    }

    public static RolePermission grant(
            Role role,
            Permission permission,
            UUID grantedBy
    ) {

        RolePermission mapping = new RolePermission();

        mapping.id = new RolePermissionId(
                role.getId(),
                permission.getId()
        );

        mapping.role = role;
        mapping.permission = permission;
        mapping.grantedBy = grantedBy;

        return mapping;
    }

    @PrePersist
    private void prePersist() {
        if (grantedAt == null) {
            grantedAt = Instant.now();
        }
    }

    public RolePermissionId getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public Permission getPermission() {
        return permission;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }
}
