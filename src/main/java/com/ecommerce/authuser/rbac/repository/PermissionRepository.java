package com.ecommerce.authuser.rbac.repository;

import com.ecommerce.authuser.rbac.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByPermissionKey(String permissionKey);

    boolean existsByPermissionKey(String permissionKey);
}
