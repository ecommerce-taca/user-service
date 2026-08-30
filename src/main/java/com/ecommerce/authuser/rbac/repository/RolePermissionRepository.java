package com.ecommerce.authuser.rbac.repository;

import com.ecommerce.authuser.rbac.domain.RolePermission;
import com.ecommerce.authuser.rbac.domain.RolePermissionId;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findAllByRole_Id(UUID roleId);

    List<RolePermission> findAllByPermission_Id(UUID permissionId);
}
