package com.ecommerce.authuser.rbac.repository;

import com.ecommerce.authuser.rbac.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
