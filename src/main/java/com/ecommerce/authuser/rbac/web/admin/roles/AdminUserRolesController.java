package com.ecommerce.authuser.rbac.web.admin.roles;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.rbac.application.admin.roles.GetAdminUserRolesQuery;
import com.ecommerce.authuser.rbac.application.admin.roles.GetAdminUserRolesResult;
import com.ecommerce.authuser.rbac.application.admin.roles.GetAdminUserRolesService;

import com.ecommerce.authuser.rbac.exception.AdminRbacPermissionDeniedException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserRolesController {

    private final GetAdminUserRolesService getAdminUserRolesService;

    @GetMapping("/{userId}/roles")
    public ResponseEntity<AdminUserRolesResponse> getRoles(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("userId") String userId,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        UUID actorUserId = parseActorUserId(jwt);

        UUID targetUserId = parseTargetUserId(userId);

        GetAdminUserRolesResult result =
                getAdminUserRolesService.get(
                        new GetAdminUserRolesQuery(
                                actorUserId,
                                targetUserId
                        )
                );

        return ResponseEntity.ok(
                toResponse(
                        result,
                        requestId
                )
        );
    }

    private UUID parseActorUserId(Jwt jwt) {
        if (jwt == null
                || jwt.getSubject() == null
                || jwt.getSubject().isBlank()) {
            throw new AdminRbacPermissionDeniedException();
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new AdminRbacPermissionDeniedException();
        }
    }

    private UUID parseTargetUserId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private AdminUserRolesResponse toResponse(
            GetAdminUserRolesResult result,
            String requestId
    ) {

        return new AdminUserRolesResponse(
                new AdminUserRolesResponse.Data(
                        result.userId(),

                        result.assignments()
                                .stream()
                                .map(this::toAssignmentResponse)
                                .toList()
                ),

                new AdminUserRolesResponse.Meta(
                        resolveRequestId(
                                requestId
                        )
                )
        );
    }

    private AdminUserRolesResponse.Assignment toAssignmentResponse(
            GetAdminUserRolesResult.AssignmentResult assignment
    ) {
        return new AdminUserRolesResponse.Assignment(
                assignment.role(),
                assignment.scopeType(),
                assignment.shopId(),
                assignment.permissions(),
                assignment.grantedAt(),
                assignment.grantedBy()
        );
    }

    private String resolveRequestId(String requestId) {
        if (requestId != null
                && !requestId.isBlank()
                && requestId.length() <= 64) {

            return requestId;
        }

        return UuidV7Generator
                .generate()
                .toString();
    }
}
