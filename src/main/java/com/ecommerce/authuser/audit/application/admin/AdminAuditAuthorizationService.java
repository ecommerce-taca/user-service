package com.ecommerce.authuser.audit.application.admin;

import com.ecommerce.authuser.audit.domain.AuditTargetType;

import com.ecommerce.authuser.rbac.application.admin.AdminRbacAuthorizationService;
import com.ecommerce.authuser.rbac.domain.RbacKeys;
import com.ecommerce.authuser.rbac.exception.AdminRbacPermissionDeniedException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuditAuthorizationService {

    private final AdminRbacAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public Set<AuditTargetType> resolveAllowedTargetTypes(
            UUID actorUserId
    ) {

        if (actorUserId == null) {
            throw new AdminRbacPermissionDeniedException();
        }

        EnumSet<AuditTargetType> allowed =
                EnumSet.noneOf(AuditTargetType.class);

        if (authorizationService
                .hasSystemPermissionOrSuperAdmin(
                        actorUserId,
                        RbacKeys.Permissions.USER_READ
                )) {

            allowed.add(AuditTargetType.USER);
        }

        if (authorizationService
                .hasSystemPermissionOrSuperAdmin(
                        actorUserId,
                        RbacKeys.Permissions.KYC_READ
                )) {

            allowed.add(AuditTargetType.KYC);
        }

        if (authorizationService
                .hasSystemPermissionOrSuperAdmin(
                        actorUserId,
                        RbacKeys.Permissions.ROLE_READ
                )) {

            allowed.add(AuditTargetType.ROLE);
        }

        if (authorizationService
                .hasSystemPermissionOrSuperAdmin(
                        actorUserId,
                        RbacKeys.Permissions.SHOP_READ
                )) {

            allowed.add(AuditTargetType.SHOP);
        }

        if (allowed.isEmpty()) {
            throw new AdminRbacPermissionDeniedException();
        }

        return Set.copyOf(allowed);
    }

    @Transactional(readOnly = true)
    public Set<AuditTargetType> requireTargetTypeAccess(
            UUID actorUserId,
            AuditTargetType requestedTargetType
    ) {
        Set<AuditTargetType> allowed =
                resolveAllowedTargetTypes(actorUserId);

        if (requestedTargetType == null) {
            return allowed;
        }

        if (!allowed.contains(requestedTargetType)) {
            throw new AdminRbacPermissionDeniedException();
        }

        return Set.of(
                requestedTargetType
        );
    }
}
