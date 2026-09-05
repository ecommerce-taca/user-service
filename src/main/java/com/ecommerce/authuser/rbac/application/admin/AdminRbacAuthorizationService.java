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
        if (actorUserId == null) {
            throw new AdminRbacPermissionDeniedException();
        }

        boolean hasRoleRead = userRoleRepository
                .existsActivePermission(
                        actorUserId,
                        RbacKeys.Permissions.ROLE_READ,
                        ScopeType.SYSTEM
                );

        if (hasRoleRead) {
            return;
        }

        long superAdminCount = userRoleRepository
                .countActiveRoles(
                        actorUserId,
                        List.of(RbacKeys.Roles.SUPER_ADMIN),
                        ScopeType.SYSTEM
                );

        if (superAdminCount == 0) {
            throw new AdminRbacPermissionDeniedException();
        }
    }
}
