package com.ecommerce.authuser.kyc.application.admin;

import com.ecommerce.authuser.kyc.exception.AdminKycPermissionDeniedException;

import com.ecommerce.authuser.rbac.domain.RbacKeys;
import com.ecommerce.authuser.rbac.domain.ScopeType;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminKycAuthorizationService {

    private final UserRoleRepository userRoleRepository;

    @Transactional(readOnly = true)
    public void requireKycRead(UUID userId) {
        requirePermission(
                userId,
                RbacKeys.Permissions.KYC_READ
        );
    }

    private void requirePermission(
            UUID userId,
            String permissionKey
    ) {

        if (userId == null
                || permissionKey == null
                || permissionKey.isBlank()) {

            throw new AdminKycPermissionDeniedException();
        }

        boolean allowed =
                userRoleRepository
                        .existsActivePermission(
                                userId,
                                permissionKey,
                                ScopeType.SYSTEM
                        );

        if (!allowed) {
            throw new AdminKycPermissionDeniedException();
        }
    }

    public void requireKycDecide(UUID userId) {
        requirePermission(
                userId,
                RbacKeys.Permissions.KYC_DECIDE
        );

        long allowedRoles =
                userRoleRepository.countActiveRoles(
                        userId,
                        List.of(
                                RbacKeys.Roles.RISK_MANAGER,
                                RbacKeys.Roles.SUPER_ADMIN
                        ),
                        ScopeType.SYSTEM
                );

        if (allowedRoles == 0) {
            throw new AdminKycPermissionDeniedException();
        }
    }
}
