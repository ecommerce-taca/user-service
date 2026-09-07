package com.ecommerce.authuser.rbac.application.admin.roles.assignment;

import com.ecommerce.authuser.rbac.application.admin.AdminRbacAuthorizationService;
import com.ecommerce.authuser.rbac.domain.RbacKeys;
import com.ecommerce.authuser.rbac.domain.Role;
import com.ecommerce.authuser.rbac.domain.ScopeType;
import com.ecommerce.authuser.rbac.exception.AdminRbacPermissionDeniedException;
import com.ecommerce.authuser.rbac.exception.InvalidRoleAssignmentException;
import com.ecommerce.authuser.rbac.repository.RolePermissionRepository;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminRoleAssignmentPolicy {

    private static final Set<String> SYSTEM_ADMIN_ROLES =
            Set.of(
                    RbacKeys.Roles.SUPER_ADMIN,
                    RbacKeys.Roles.RISK_MANAGER,
                    RbacKeys.Roles.CATALOG_ADMIN,
                    RbacKeys.Roles.FINANCE_OPS,
                    RbacKeys.Roles.SUPPORT_VIEWER
            );

    private final AdminRbacAuthorizationService authorizationService;

    private final UserRoleRepository userRoleRepository;

    private final RolePermissionRepository rolePermissionRepository;

    public void validateTargetRole(
            Role role,
            UUID shopId
    ) {
        if (role == null) {
            throw new InvalidRoleAssignmentException();
        }

        String roleKey = role.getRoleKey();

        if (RbacKeys.Roles.SELLER_STAFF.equals(roleKey)) {

            if (role.getScopeType() != ScopeType.SHOP || shopId == null) {
                throw new InvalidRoleAssignmentException();
            }

            return;
        }

        if (SYSTEM_ADMIN_ROLES.contains(roleKey)) {
            if (role.getScopeType() != ScopeType.SYSTEM || shopId != null) {
                throw new InvalidRoleAssignmentException();
            }

            return;
        }

        throw new InvalidRoleAssignmentException();
    }

    public void requireActorMayMutate(
            UUID actorUserId,
            Role targetRole,
            UUID shopId
    ) {

        if (authorizationService.isSuperAdmin(actorUserId)) {
            return;
        }

        if (targetRole.getScopeType() == ScopeType.SYSTEM) {
            throw new AdminRbacPermissionDeniedException();
        }

        if (!RbacKeys.Roles.SELLER_STAFF.equals(
                targetRole.getRoleKey()
        )) {
            throw new AdminRbacPermissionDeniedException();
        }

        List<String> targetPermissions = rolePermissionRepository
                .findPermissionKeysByRoleId(targetRole.getId());

        Set<String> actorPermissions =
                new HashSet<>(
                        userRoleRepository
                                .findActivePermissionKeysForShop(
                                        actorUserId,
                                        shopId
                                )
                );

        if (!actorPermissions.containsAll(
                targetPermissions
        )) {
            throw new AdminRbacPermissionDeniedException();
        }
    }
}