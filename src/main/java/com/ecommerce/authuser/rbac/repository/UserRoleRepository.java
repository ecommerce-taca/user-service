package com.ecommerce.authuser.rbac.repository;

import com.ecommerce.authuser.rbac.domain.ScopeType;
import com.ecommerce.authuser.rbac.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findAllByUser_IdAndRevokedAtIsNull(UUID userId);

    List<UserRole> findAllByShop_IdAndRevokedAtIsNull(UUID shopId);

    Optional<UserRole> findByUser_IdAndRole_IdAndShop_Id(
            UUID userId,
            UUID roleId,
            UUID shopId
    );

    Optional<UserRole> findByUser_IdAndRole_IdAndShopIsNull(
            UUID userId,
            UUID roleId
    );

    boolean existsByUser_IdAndRole_RoleKeyAndRevokedAtIsNull(
            UUID userId,
            String roleKey
    );

    boolean existsByUser_IdAndRole_RoleKeyAndShop_IdAndRevokedAtIsNull(
            UUID userId,
            String roleKey,
            UUID shopId
    );

    @Query("""
        select (count(ur) > 0)
        from UserRole ur, RolePermission rp
        where ur.user.id = :userId
            and ur.revokedAt is null
            and ur.role.id = rp.role.id
            and ur.role.scopeType = :scopeType
            and rp.permission.permissionKey = :permissionKey
        """)
    boolean existsActivePermission(
            @Param("userId") UUID userId,
            @Param("permissionKey") String permissionKey,
            @Param("scopeType") ScopeType scopeType
    );
}
