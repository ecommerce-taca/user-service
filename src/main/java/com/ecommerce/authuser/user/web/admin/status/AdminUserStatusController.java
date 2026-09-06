package com.ecommerce.authuser.user.web.admin.status;

import com.ecommerce.authuser.auth.exception.mfa.MfaAuthenticationRequiredException;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.user.application.admin.status.AdminUserStatusCommand;
import com.ecommerce.authuser.user.application.admin.status.AdminUserStatusResult;
import com.ecommerce.authuser.user.application.admin.status.AdminUserStatusService;

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
public class AdminUserStatusController {

    private final AdminUserStatusService adminUserStatusService;

    @PatchMapping("/{userId}/status")
    public ResponseEntity<AdminUserStatusResponse> changeStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("userId") String userId,
            @RequestBody(required = false) AdminUserStatusRequest request,
            @RequestHeader(name = "X-MFA-Step-Up", required = false) String stepUpToken,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            HttpServletRequest httpRequest
    ) {

        UUID actorUserId = parseActorUserId(jwt);

        UUID sessionId = parseSessionId(jwt);

        UUID targetUserId = parseTargetUserId(userId);

        String status = request == null ? null : request.status();

        String reason = request == null ? null : request.reason();

        AdminUserStatusResult result =
                adminUserStatusService.change(
                        new AdminUserStatusCommand(
                                actorUserId,
                                sessionId,
                                targetUserId,
                                status,
                                reason,
                                stepUpToken,
                                httpRequest.getRemoteAddr()
                        )
                );

        return ResponseEntity.ok(
                new AdminUserStatusResponse(
                        new AdminUserStatusResponse.Data(
                                result.userId(),
                                result.newStatus(),
                                result.changedAt()
                        ),

                        new AdminUserStatusResponse.Meta(
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
