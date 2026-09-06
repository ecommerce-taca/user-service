package com.ecommerce.authuser.rbac.application.admin.roles.assignment;

import com.ecommerce.authuser.audit.domain.AuditLog;
import com.ecommerce.authuser.audit.domain.AuditTargetType;
import com.ecommerce.authuser.audit.repository.AuditLogRepository;

import com.ecommerce.authuser.auth.exception.MfaStepUpRequiredException;

import com.ecommerce.authuser.mfa.application.admin.AdminStepUpService;
import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;

import com.ecommerce.authuser.rbac.application.admin.AdminRbacAuthorizationService;

import com.ecommerce.authuser.rbac.domain.RbacKeys;
import com.ecommerce.authuser.rbac.domain.Role;
import com.ecommerce.authuser.rbac.domain.ScopeType;
import com.ecommerce.authuser.rbac.domain.UserRole;

import com.ecommerce.authuser.rbac.exception.AdminRbacPermissionDeniedException;
import com.ecommerce.authuser.rbac.exception.InvalidRoleAssignmentException;
import com.ecommerce.authuser.rbac.exception.RoleAssignmentExistsException;
import com.ecommerce.authuser.rbac.exception.RoleAssignmentNotFoundException;

import com.ecommerce.authuser.rbac.repository.RoleRepository;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import com.ecommerce.authuser.security.service.AuditValueHasher;

import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.shop.repository.ShopRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.exception.profile.UserNotFoundException;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminRoleAssignmentService {

    private static final short EVENT_SCHEMA_VERSION = 1;

    private final AdminRbacAuthorizationService authorizationService;

    private final AdminStepUpService stepUpService;

    private final AdminRoleAssignmentPolicy assignmentPolicy;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserRoleRepository userRoleRepository;

    private final ShopRepository shopRepository;

    private final AuditLogRepository auditLogRepository;

    private final OutboxEventRepository outboxEventRepository;

    private final AuditValueHasher auditValueHasher;

    @Transactional(
            noRollbackFor =
                    MfaStepUpRequiredException.class
    )
    public AdminRoleAssignmentResult change(
            AdminRoleAssignmentCommand command
    ) {

        if (command == null || command.actorUserId() == null) {
            throw new AdminRbacPermissionDeniedException();
        }

        authorizationService.requireRoleAssign(command.actorUserId());

        Instant now = Instant.now();

        stepUpService.require(
                command.actorUserId(),
                command.sessionId(),
                command.stepUpToken(),
                now
        );

        authorizationService.requireRoleAssign(command.actorUserId());

        AdminRoleAssignmentAction action = parseAction(command.action());

        String reason = normalizeReason(command.reason());

        String roleKey = normalizeRoleKey(command.role());

        Role targetRole = roleRepository
                .findByRoleKey(roleKey)
                .orElseThrow(InvalidRoleAssignmentException::new);

        assignmentPolicy.validateTargetRole(
                targetRole,
                command.shopId()
        );

        if (command.targetUserId() == null) {
            throw new UserNotFoundException();
        }

        validateSelfMutation(
                command.actorUserId(),
                command.targetUserId(),
                action,
                targetRole
        );

        User targetUser = userRepository
                .findByIdForUpdate(command.targetUserId())
                .orElseThrow(UserNotFoundException::new);

        Shop shop = resolveShopForUpdate(
                targetRole,
                command.shopId()
        );


        assignmentPolicy.requireActorMayMutate(
                command.actorUserId(),
                targetRole,
                shop == null
                        ? null
                        : shop.getId()
        );

        Optional<UserRole> existing =
                findAssignmentForUpdate(
                        targetUser.getId(),
                        targetRole,
                        shop
                );

        UserRole assignment =
                switch (action) {

                    case GRANT ->
                            grant(
                                    targetUser,
                                    targetRole,
                                    shop,
                                    existing,
                                    command.actorUserId(),
                                    now
                            );

                    case REVOKE ->
                            revoke(
                                    existing,
                                    now
                            );
                };

        userRoleRepository.saveAndFlush(assignment);

        createAudit(
                command,
                targetRole,
                assignment,
                shop,
                action,
                reason,
                now
        );

        createOutboxEvent(
                targetUser,
                targetRole,
                shop,
                action
        );

        return new AdminRoleAssignmentResult(
                targetUser.getId(),
                targetRole.getRoleKey(),
                targetRole.getScopeType(),
                shop == null ? null : shop.getId(),
                action,
                now
        );
    }

    private AdminRoleAssignmentAction parseAction(String value) {

        if (value == null || value.isBlank()) {
            throw new InvalidRoleAssignmentException();
        }

        try {
            return AdminRoleAssignmentAction.valueOf(
                    value.strip()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (IllegalArgumentException ex) {
            throw new InvalidRoleAssignmentException();
        }
    }

    private String normalizeRoleKey(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRoleAssignmentException();
        }

        String normalized = value.strip().toUpperCase(Locale.ROOT);

        if (normalized.length() > 32) {
            throw new InvalidRoleAssignmentException();
        }

        return normalized;
    }

    private String normalizeReason(String value) {

        if (value == null || value.isBlank()) {
            throw new InvalidRoleAssignmentException();
        }

        String normalized = value.strip();

        int length =
                normalized.codePointCount(
                        0,
                        normalized.length()
                );

        if (length < 10 || length > 1000) {
            throw new InvalidRoleAssignmentException();
        }

        return normalized;
    }

    private void validateSelfMutation(
            UUID actorUserId,
            UUID targetUserId,
            AdminRoleAssignmentAction action,
            Role targetRole
    ) {

        if (!actorUserId.equals(
                targetUserId
        )) {
            return;
        }

        if (action == AdminRoleAssignmentAction.GRANT) {
            throw new AdminRbacPermissionDeniedException();
        }

        if (action
                == AdminRoleAssignmentAction.REVOKE
                && RbacKeys.Roles.SUPER_ADMIN.equals(
                targetRole.getRoleKey()
        )) {
            throw new AdminRbacPermissionDeniedException();
        }
    }

    private Shop resolveShopForUpdate(
            Role targetRole,
            UUID shopId
    ) {

        if (targetRole.getScopeType()
                != ScopeType.SHOP) {

            return null;
        }

        return shopRepository
                .findByIdForUpdate(shopId)
                .orElseThrow(InvalidRoleAssignmentException::new);
    }

    private Optional<UserRole>
    findAssignmentForUpdate(
            UUID userId,
            Role role,
            Shop shop
    ) {

        if (role.getScopeType() == ScopeType.SHOP) {
            return userRoleRepository
                    .findShopAssignmentForUpdate(
                            userId,
                            role.getId(),
                            shop.getId()
                    );
        }

        return userRoleRepository
                .findUnscopedAssignmentForUpdate(
                        userId,
                        role.getId()
                );
    }

    private UserRole grant(
            User targetUser,
            Role targetRole,
            Shop shop,
            Optional<UserRole> existing,
            UUID actorUserId,
            Instant now
    ) {

        if (existing.isPresent()) {

            UserRole assignment = existing.get();

            if (assignment.isActive()) {
                throw new RoleAssignmentExistsException();
            }

            assignment.reactivate(
                    actorUserId,
                    now
            );

            return assignment;
        }

        return UserRole.assign(
                targetUser,
                targetRole,
                shop,
                actorUserId
        );
    }

    private UserRole revoke(
            Optional<UserRole> existing,
            Instant now
    ) {

        UserRole assignment =
                existing.orElseThrow(
                        RoleAssignmentNotFoundException::new
                );

        if (!assignment.isActive()) {
            throw new RoleAssignmentNotFoundException();
        }

        assignment.revoke(now);

        return assignment;
    }

    private void createAudit(
            AdminRoleAssignmentCommand command,
            Role targetRole,
            UserRole assignment,
            Shop shop,
            AdminRoleAssignmentAction action,
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

        metadata.put(
                "assignment_id",
                assignment.getId().toString()
        );

        metadata.put(
                "target_user_id",
                command.targetUserId().toString()
        );

        metadata.put(
                "role",
                targetRole.getRoleKey()
        );

        metadata.put(
                "scope_type",
                targetRole
                        .getScopeType()
                        .name()
        );

        metadata.put(
                "action",
                action.name()
        );

        if (shop != null) {
            metadata.put(
                    "shop_id",
                    shop.getId().toString()
            );
        }

        AuditLog audit =
                AuditLog.create(
                        command.actorUserId(),
                        action
                                == AdminRoleAssignmentAction.GRANT
                                ? "ROLE_GRANTED"
                                : "ROLE_REVOKED",

                        AuditTargetType.ROLE,
                        targetRole.getId(),
                        reason,
                        metadata,
                        ipHash,
                        now
                );

        auditLogRepository.save(audit);
    }

    private void createOutboxEvent(
            User targetUser,
            Role targetRole,
            Shop shop,
            AdminRoleAssignmentAction action
    ) {

        Map<String, Object> payload =
                new LinkedHashMap<>();

        payload.put(
                "user_id",
                targetUser.getId().toString()
        );

        payload.put(
                "role",
                targetRole.getRoleKey()
        );

        payload.put(
                "scope_type",
                targetRole
                        .getScopeType()
                        .name()
        );

        if (shop != null) {
            payload.put(
                    "shop_id",
                    shop.getId().toString()
            );
        }

        payload.put(
                "action",

                action
                        == AdminRoleAssignmentAction.GRANT
                        ? "GRANTED"
                        : "REVOKED"
        );

        OutboxEvent event =
                OutboxEvent.create(
                        OutboxAggregateType.USER,
                        targetUser.getId(),
                        "user.role_changed",
                        EVENT_SCHEMA_VERSION,
                        targetUser.getId().toString(),
                        payload
                );

        outboxEventRepository.save(event);
    }
}
