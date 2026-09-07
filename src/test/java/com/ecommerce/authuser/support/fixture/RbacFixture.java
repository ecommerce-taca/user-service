package com.ecommerce.authuser.support.fixture;

import com.ecommerce.authuser.rbac.domain.Permission;
import com.ecommerce.authuser.rbac.domain.Role;
import com.ecommerce.authuser.rbac.domain.RolePermission;
import com.ecommerce.authuser.rbac.repository.PermissionRepository;
import com.ecommerce.authuser.rbac.repository.RolePermissionRepository;
import com.ecommerce.authuser.rbac.repository.RoleRepository;
import com.ecommerce.authuser.support.testdata.RbacTestData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class RbacFixture {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Autowired
    public RbacFixture(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public Role createRole(
            Role role) {
        return roleRepository.saveAndFlush(role);
    }

    public Permission createPermission(
            Permission permission) {
        return permissionRepository.saveAndFlush(permission);
    }

    public RolePermission grantPermission(
            Role role,
            Permission permission) {
        Role persistedRole = roleRepository.saveAndFlush(role);
        Permission persistedPermission = permissionRepository.saveAndFlush(permission);

        RolePermission rolePermission = RbacTestData.grant(
                persistedRole,
                persistedPermission);

        return rolePermissionRepository.saveAndFlush(
                rolePermission);
    }

    public RolePermission grantPermission(
            Role role,
            Permission permission,
            UUID grantedBy) {
        Role persistedRole = roleRepository.saveAndFlush(role);
        Permission persistedPermission = permissionRepository.saveAndFlush(permission);

        RolePermission rolePermission = RbacTestData.grant(
                persistedRole,
                persistedPermission,
                grantedBy);

        return rolePermissionRepository.saveAndFlush(
                rolePermission);
    }

    public Role createBuyerRole() {
        return createRole(
                RbacTestData.buyerRole());
    }

    public Role createUserRole(
            String roleKey) {
        return createRole(
                RbacTestData.userRole(
                        roleKey));
    }

    public Permission createUserReadPermission() {
        return createPermission(
                RbacTestData.userReadPermission());
    }

    public Permission createUserUpdatePermission() {
        return createPermission(
                RbacTestData.userUpdatePermission());
    }

    public Permission createUserCreatePermission() {
        return createPermission(
                RbacTestData.userCreatePermission());
    }

    public Permission createPermission(
            String permissionKey) {
        return createPermission(
                RbacTestData.permission(
                        permissionKey));
    }
}
