package com.ecommerce.authuser.user.application.admin.status;

import com.ecommerce.authuser.audit.domain.AuditLog;
import com.ecommerce.authuser.audit.domain.AuditTargetType;
import com.ecommerce.authuser.audit.repository.AuditLogRepository;

import com.ecommerce.authuser.auth.exception.MfaStepUpRequiredException;

import com.ecommerce.authuser.mfa.application.admin.AdminStepUpService;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;

import com.ecommerce.authuser.rbac.application.admin.AdminRbacAuthorizationService;
import com.ecommerce.authuser.rbac.exception.AdminRbacPermissionDeniedException;

import com.ecommerce.authuser.security.service.AuditValueHasher;

import com.ecommerce.authuser.token.domain.RefreshToken;
import com.ecommerce.authuser.token.domain.TokenRevokeReason;
import com.ecommerce.authuser.token.repository.RefreshTokenRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.domain.UserStatus;

import com.ecommerce.authuser.user.exception.admin.AdminUserStatusConflictException;
import com.ecommerce.authuser.user.exception.admin.InvalidAdminUserStatusRequestException;
import com.ecommerce.authuser.user.exception.profile.UserNotFoundException;

import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserStatusService {

    private static final short EVENT_SCHEMA_VERSION = 1;

    private final AdminRbacAuthorizationService authorizationService;

    private final AdminStepUpService stepUpService;

    private final UserRepository userRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final AuditLogRepository auditLogRepository;

    private final OutboxEventRepository outboxEventRepository;

    private final AuditValueHasher auditValueHasher;

    private final RevokedUserCache revokedUserCache;

    @Transactional(
            noRollbackFor =
                    MfaStepUpRequiredException.class
    )
    public AdminUserStatusResult change(AdminUserStatusCommand command) {

        if (command == null || command.actorUserId() == null) {
            throw new AdminRbacPermissionDeniedException();
        }

        authorizationService.requireUserSuspend(
                command.actorUserId()
        );

        Instant now = Instant.now();

        stepUpService.require(
                command.actorUserId(),
                command.sessionId(),
                command.stepUpToken(),
                now
        );

        authorizationService.requireUserSuspend(command.actorUserId());

        AdminUserStatusChange requestedStatus = parseStatus(command.status());

        String reason = normalizeReason(command.reason());

        if (command.targetUserId() == null) {
            throw new UserNotFoundException();
        }

        if (command.actorUserId().equals(
                command.targetUserId()
        )) {
            throw new AdminRbacPermissionDeniedException();
        }

        User targetUser = userRepository
                .findByIdForUpdate(command.targetUserId())
                .orElseThrow(UserNotFoundException::new);

        UserStatus oldStatus = targetUser.getStatus();

        int revokedRefreshTokens =
                switch (requestedStatus) {

                    case SUSPENDED ->
                            suspend(targetUser, now);

                    case ACTIVE ->
                            restore(targetUser);
                };

        UserStatus newStatus = targetUser.getStatus();

        userRepository.saveAndFlush(targetUser);

        createAudit(
                command,
                targetUser,
                oldStatus,
                newStatus,
                revokedRefreshTokens,
                reason,
                now
        );

        createOutboxEvent(
                targetUser,
                oldStatus,
                newStatus,
                reason
        );

        if (newStatus == UserStatus.SUSPENDED) {
            revokedUserCache.markRevoked(targetUser.getId(), now);
        }

        return new AdminUserStatusResult(
                targetUser.getId(),
                oldStatus,
                newStatus,
                now,
                revokedRefreshTokens
        );
    }

    private AdminUserStatusChange parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidAdminUserStatusRequestException();
        }

        try {
            return AdminUserStatusChange.valueOf(
                    value.strip()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException ex) {
            throw new InvalidAdminUserStatusRequestException();
        }
    }

    private String normalizeReason(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidAdminUserStatusRequestException();
        }

        String normalized = value.strip();

        int length =
                normalized.codePointCount(0, normalized.length());

        if (length < 10 || length > 1000) {
            throw new InvalidAdminUserStatusRequestException();
        }

        return normalized;
    }

    private int suspend(
            User user,
            Instant now
    ) {
        if (user.getStatus() != UserStatus.ACTIVE
                && user.getStatus() != UserStatus.LOCKED) {
            throw new AdminUserStatusConflictException();
        }

        try {
            user.suspend();
        } catch (IllegalStateException ex) {
            throw new AdminUserStatusConflictException();
        }

        List<RefreshToken> activeTokens =
                refreshTokenRepository
                        .findAllActiveByUserForUpdate(
                                user.getId()
                        );

        for (RefreshToken token : activeTokens) {

            token.revoke(TokenRevokeReason.SUSPEND, now);
        }

        if (!activeTokens.isEmpty()) {
            refreshTokenRepository.saveAll(activeTokens);
        }

        return activeTokens.size();
    }

    private int restore(User user) {
        if (user.getStatus() != UserStatus.SUSPENDED) {
            throw new AdminUserStatusConflictException();
        }

        try {
            user.restore();
        } catch (IllegalStateException ex) {
            throw new AdminUserStatusConflictException();
        }

        return 0;
    }

    private void createAudit(
            AdminUserStatusCommand command,
            User targetUser,
            UserStatus oldStatus,
            UserStatus newStatus,
            int revokedRefreshTokens,
            String reason,
            Instant now
    ) {

        String clientIp =
                command.clientIp() == null
                        || command.clientIp().isBlank()
                        ? "unknown"
                        : command.clientIp().strip();

        String ipHash = auditValueHasher.hash(clientIp);

        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put("old_status", oldStatus.name());
        metadata.put("new_status", newStatus.name());
        metadata.put(
                "revoked_refresh_tokens",
                revokedRefreshTokens
        );

        AuditLog audit =
                AuditLog.create(
                        command.actorUserId(),

                        newStatus == UserStatus.SUSPENDED
                                ? "USER_SUSPENDED"
                                : "USER_RESTORED",

                        AuditTargetType.USER,
                        targetUser.getId(),
                        reason,
                        metadata,
                        ipHash,
                        now
                );

        auditLogRepository.save(audit);
    }

    private void createOutboxEvent(
            User targetUser,
            UserStatus oldStatus,
            UserStatus newStatus,
            String reason
    ) {

        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put(
                "user_id",
                targetUser.getId().toString()
        );

        payload.put("old_status", oldStatus.name());
        payload.put("new_status", newStatus.name());
        payload.put("reason", reason);

        OutboxEvent event =
                OutboxEvent.create(
                        OutboxAggregateType.USER,
                        targetUser.getId(),
                        "user.status_changed",
                        EVENT_SCHEMA_VERSION,
                        targetUser
                                .getId()
                                .toString(),
                        payload
                );

        outboxEventRepository.save(event);
    }
}