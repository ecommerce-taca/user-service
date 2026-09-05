package com.ecommerce.authuser.rbac.application.admin;

import com.ecommerce.authuser.rbac.domain.RbacKeys;
import com.ecommerce.authuser.rbac.domain.ScopeType;

import com.ecommerce.authuser.rbac.exception.AdminRbacPermissionDeniedException;

import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminRbacAuthorizationService {

    private final UserRoleRepository userRoleRepository;

    @Transactional(readOnly = true)
    public void requireRoleRead(UUID actorUserId) {
        requireSystemPermissionOrSuperAdmin(
                actorUserId,
                RbacKeys.Permissions.ROLE_READ
        );
    }

    @Transactional(readOnly = true)
    public void requireRoleAssign(UUID actorUserId) {
        requireSystemPermissionOrSuperAdmin(
                actorUserId,
                RbacKeys.Permissions.ROLE_ASSIGN
        );
    }

    @Transactional(readOnly = true)
    public boolean isSuperAdmin(UUID actorUserId) {
        if (actorUserId == null) {
            return false;
        }

        return userRoleRepository.countActiveRoles(
                actorUserId,
                List.of(RbacKeys.Roles.SUPER_ADMIN),
                ScopeType.SYSTEM
        ) > 0;
    }

    @Transactional(readOnly = true)
    public void requireUserSuspend(UUID actorUserId) {
        requireSystemPermissionOrSuperAdmin(
                actorUserId,
                RbacKeys.Permissions.USER_SUSPEND
        );
    }

    @Transactional(readOnly = true)
    public boolean hasSystemPermissionOrSuperAdmin(
            UUID actorUserId,
            String permissionKey
    ) {

        if (actorUserId == null
                || permissionKey == null
                || permissionKey.isBlank()) {
            return false;
        }

        if (userRoleRepository.existsActivePermission(
                actorUserId,
                permissionKey,
                ScopeType.SYSTEM
        )) {
            return true;
        }

        return isSuperAdmin(actorUserId);
    }


    private void requireSystemPermissionOrSuperAdmin(
            UUID actorUserId,
            String permissionKey
    ) {

        if (!hasSystemPermissionOrSuperAdmin(
                actorUserId,
                permissionKey
        )) {

            throw new AdminRbacPermissionDeniedException();
        }
    }
}
