package com.ecommerce.authuser.rbac.repository;

import com.ecommerce.authuser.rbac.domain.RolePermission;
import com.ecommerce.authuser.rbac.domain.RolePermissionId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findAllByRole_Id(UUID roleId);

    List<RolePermission> findAllByPermission_Id(UUID permissionId);

    @Query("""
        select rp
        from RolePermission rp
        join fetch rp.permission permission
        where rp.role.id in :roleIds
        order by rp.role.id asc,
                permission.permissionKey asc
        """)
    List<RolePermission> findAllByRoleIdsWithPermission(
            @Param("roleIds")
            Collection<UUID> roleIds
    );
}
