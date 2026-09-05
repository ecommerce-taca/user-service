package com.ecommerce.authuser.rbac.web.admin.roles.assignment;

import com.ecommerce.authuser.auth.exception.mfa.MfaAuthenticationRequiredException;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.rbac.application.admin.roles.assignment.AdminRoleAssignmentCommand;
import com.ecommerce.authuser.rbac.application.admin.roles.assignment.AdminRoleAssignmentResult;
import com.ecommerce.authuser.rbac.application.admin.roles.assignment.AdminRoleAssignmentService;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminRoleAssignmentController {

    private static final UUID INVALID_UUID_SENTINEL = new UUID(0L, 0L);

    private final AdminRoleAssignmentService adminRoleAssignmentService;

    @PatchMapping("/{userId}/roles")
    public ResponseEntity<AdminRoleAssignmentResponse> changeRole(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("userId") String userId,
            @RequestBody(required = false) AdminRoleAssignmentRequest request,
            @RequestHeader(name = "X-MFA-Step-Up", required = false) String stepUpToken,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            HttpServletRequest httpRequest
    ) {

        UUID actorUserId = parseActorUserId(jwt);

        UUID sessionId = parseSessionId(jwt);

        UUID targetUserId = parseTargetUserId(userId);

        String action = request == null ? null : request.action();

        String role = request == null ? null : request.role();

        UUID shopId = request == null ? null : parseShopId(request.shopId());

        String reason = request == null ? null : request.reason();

        AdminRoleAssignmentResult result =
                adminRoleAssignmentService.change(
                        new AdminRoleAssignmentCommand(
                                actorUserId,
                                sessionId,
                                targetUserId,
                                action,
                                role,
                                shopId,
                                reason,
                                stepUpToken,
                                httpRequest.getRemoteAddr()
                        )
                );

        return ResponseEntity.ok(
                new AdminRoleAssignmentResponse(
                        new AdminRoleAssignmentResponse.Data(
                                result.userId(),
                                result.role(),
                                result.scopeType(),
                                result.shopId(),
                                result.action(),
                                result.changedAt()
                        ),

                        new AdminRoleAssignmentResponse.Meta(
                                resolveRequestId(
                                        requestId
                                )
                        )
                )
        );
    }

    private UUID parseActorUserId(Jwt jwt) {
        if (jwt == null
                || jwt.getSubject() == null
                || jwt.getSubject().isBlank()) {
            throw new MfaAuthenticationRequiredException();
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException ex) {
            throw new MfaAuthenticationRequiredException();
        }
    }

    private UUID parseSessionId(Jwt jwt) {
        if (jwt == null) {
            throw new MfaAuthenticationRequiredException();
        }

        String value = jwt.getClaimAsString("session_id");

        if (value == null || value.isBlank()) {
            throw new MfaAuthenticationRequiredException();
        }

        try {
            return UUID.fromString(value);
        } catch (RuntimeException ex) {
            throw new MfaAuthenticationRequiredException();
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

    private UUID parseShopId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return INVALID_UUID_SENTINEL;
        }
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
