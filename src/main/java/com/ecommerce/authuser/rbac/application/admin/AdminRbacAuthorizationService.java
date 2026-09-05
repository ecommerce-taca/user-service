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

    private void requireSystemPermissionOrSuperAdmin(
            UUID actorUserId,
            String permissionKey
    ) {
        if (actorUserId == null) {
            throw new AdminRbacPermissionDeniedException();
        }

        boolean hasPermission =
                userRoleRepository.existsActivePermission(
                        actorUserId,
                        permissionKey,
                        ScopeType.SYSTEM
                );

        if (hasPermission) {
            return;
        }

        if (isSuperAdmin(actorUserId)) {
            return;
        }

        throw new AdminRbacPermissionDeniedException();
    }
}
