package com.ecommerce.authuser.rbac.application.admin.roles;

import com.ecommerce.authuser.rbac.application.admin.AdminRbacAuthorizationService;

import com.ecommerce.authuser.rbac.domain.RolePermission;
import com.ecommerce.authuser.rbac.domain.UserRole;

import com.ecommerce.authuser.rbac.repository.RolePermissionRepository;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import com.ecommerce.authuser.user.exception.profile.UserNotFoundException;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAdminUserRolesService {

    private final AdminRbacAuthorizationService authorizationService;

    private final UserRepository userRepository;

    private final UserRoleRepository userRoleRepository;

    private final RolePermissionRepository rolePermissionRepository;

    @Transactional(readOnly = true)
    public GetAdminUserRolesResult get(
            GetAdminUserRolesQuery query
    ) {


        if (query == null) {
            throw new IllegalArgumentException(
                    "query must not be null"
            );
        }

        authorizationService.requireRoleRead(query.actorUserId());

        if (query.targetUserId() == null) {
            throw new UserNotFoundException();
        }

        boolean targetExists = userRepository
                .findByIdAndDeletedAtIsNull(query.targetUserId())
                .isPresent();

        if (!targetExists) {
            throw new UserNotFoundException();
        }

        List<UserRole> assignments = userRoleRepository
                .findActiveAssignmentsWithRoleAndShop(
                        query.targetUserId()
                );

        if (assignments.isEmpty()) {
            return new GetAdminUserRolesResult(
                    query.targetUserId(),
                    List.of()
            );
        }

        Set<UUID> roleIds = new LinkedHashSet<>();

        assignments.forEach(
                assignment ->
                        roleIds.add(
                                assignment
                                        .getRole()
                                        .getId()
                        )
        );

        List<RolePermission> mappings =
                rolePermissionRepository
                        .findAllByRoleIdsWithPermission(
                                roleIds
                        );

        Map<UUID, List<String>>
                permissionsByRole =
                new LinkedHashMap<>();

        for (RolePermission mapping : mappings) {

            UUID roleId =
                    mapping
                            .getRole()
                            .getId();

            permissionsByRole
                    .computeIfAbsent(
                            roleId,
                            ignored ->
                                    new ArrayList<>()
                    )
                    .add(
                            mapping
                                    .getPermission()
                                    .getPermissionKey()
                    );
        }

        List<GetAdminUserRolesResult.AssignmentResult>
                results =
                assignments
                        .stream()
                        .map(
                                assignment ->
                                        mapAssignment(
                                                assignment,
                                                permissionsByRole
                                        )
                        )
                        .toList();

        return new GetAdminUserRolesResult(
                query.targetUserId(),
                results
        );
    }

    private GetAdminUserRolesResult.AssignmentResult mapAssignment(
            UserRole assignment,
            Map<UUID, List<String>> permissionsByRole
    ) {

        UUID roleId = assignment
                .getRole()
                .getId();

        List<String> permissions =
                permissionsByRole
                        .getOrDefault(
                                roleId,
                                List.of()
                        );

        UUID shopId =
                assignment.getShop() == null
                        ? null
                        : assignment
                        .getShop()
                        .getId();

        return new GetAdminUserRolesResult.AssignmentResult(
                assignment
                        .getRole()
                        .getRoleKey(),
                assignment
                        .getRole()
                        .getScopeType(),
                shopId,
                List.copyOf(permissions),
                assignment.getGrantedAt(),
                assignment.getGrantedBy()
        );
    }
}
