package com.ecommerce.authuser.auth.application;

import com.ecommerce.authuser.audit.domain.AuditLog;
import com.ecommerce.authuser.audit.domain.AuditTargetType;
import com.ecommerce.authuser.audit.repository.AuditLogRepository;

import com.ecommerce.authuser.auth.exception.AccountSuspendedException;
import com.ecommerce.authuser.auth.exception.ResendLimitExceededException;
import com.ecommerce.authuser.auth.exception.VerificationAlreadyCompleteException;
import com.ecommerce.authuser.auth.security.SecureTokenGenerator;
import com.ecommerce.authuser.auth.security.TokenHasher;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.outbox.security.OutboxPayloadProtector;

import com.ecommerce.authuser.security.service.AuditValueHasher;

import com.ecommerce.authuser.token.domain.VerificationChannel;
import com.ecommerce.authuser.token.domain.VerificationPurpose;
import com.ecommerce.authuser.token.domain.VerificationToken;
import com.ecommerce.authuser.token.repository.VerificationTokenRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.domain.UserStatus;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailVerificationResendService {

    private static final Duration EMAIL_VERIFICATION_TTL = Duration.ofHours(24);

    private static final Duration RESEND_WINDOW = Duration.ofHours(1);

    private static final long MAX_RESENDS_PER_WINDOW = 3;

    private static final String AUDIT_ACTION = "AUTH_EMAIL_VERIFICATION_RESEND";

    private final UserRepository userRepository;

    private final VerificationTokenRepository verificationTokenRepository;

    private final SecureTokenGenerator tokenGenerator;

    private final TokenHasher tokenHasher;

    private final OutboxEventRepository outboxEventRepository;

    private final OutboxPayloadProtector outboxPayloadProtector;

    private final AuditLogRepository auditLogRepository;

    private final AuditValueHasher auditValueHasher;

    @Transactional
    public EmailResendResult resend(EmailResendCommand command) {
        Instant now = Instant.now();

        User user = userRepository
                .findByIdForUpdate(command.userId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccountSuspendedException();
        }

        if (user.getEmailVerifiedAt() != null) {
            throw new VerificationAlreadyCompleteException();
        }

        Instant windowStart = now.minus(RESEND_WINDOW);

        long resendCount = auditLogRepository
                .countByActorUserIdAndActionAndOccurredAtAfter(
                        user.getId(),
                        AUDIT_ACTION,
                        windowStart
                );

        if (resendCount >= MAX_RESENDS_PER_WINDOW) {
            throw new ResendLimitExceededException();
        }

        List<VerificationToken> activeTokens =
                verificationTokenRepository
                        .findActiveForUpdate(
                                user.getId(),
                                VerificationPurpose.EMAIL_VERIFY,
                                VerificationChannel.EMAIL
                        );

        activeTokens.forEach(token -> token.revoke(now));

        String rawVerificationToken = tokenGenerator.generate();

        String verificationHash = tokenHasher.hash(rawVerificationToken);

        Instant expiresAt = now.plus(EMAIL_VERIFICATION_TTL);

        VerificationToken newToken =
                VerificationToken.create(
                        user,
                        VerificationChannel.EMAIL,
                        VerificationPurpose.EMAIL_VERIFY,
                        verificationHash,
                        maskEmail(user.getEmail()),
                        expiresAt
                );

        verificationTokenRepository.save(newToken);

        OutboxEvent verificationEvent =
                OutboxEvent.create(
                        OutboxAggregateType.USER,

                        user.getId(),

                        "AUTH_VERIFICATION_REQUESTED",

                        (short) 1,

                        user.getId()
                                .toString(),

                        outboxPayloadProtector.protect(
                                "AUTH_VERIFICATION_REQUESTED",

                                Map.of(
                                        "command_type",
                                        "AUTH_VERIFICATION_REQUESTED",

                                        "user_id",
                                        user.getId()
                                                .toString(),

                                        "channel",
                                        "EMAIL",

                                        "recipient",
                                        user.getEmail(),

                                        "display_name",
                                        user.getFullName(),

                                        "verification_token",
                                        rawVerificationToken,

                                        "expires_at",
                                        expiresAt.toString()
                                )
                        )
                );

        outboxEventRepository.save(
                verificationEvent
        );

        String clientIp =
                command.clientIp() == null
                        || command.clientIp()
                        .isBlank()
                        ? "unknown"
                        : command.clientIp()
                        .trim();

        String ipHash =
                auditValueHasher.hash(
                        clientIp
                );

        AuditLog audit =
                AuditLog.create(
                        user.getId(),

                        AUDIT_ACTION,

                        AuditTargetType.USER,

                        user.getId(),

                        "Email verification resent",

                        Map.of(
                                "channel",
                                "EMAIL",

                                "purpose",
                                "EMAIL_VERIFY",

                                "expires_at",
                                expiresAt.toString()
                        ),

                        ipHash,

                        now
                );

        auditLogRepository.save(
                audit
        );

        return new EmailResendResult(expiresAt);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');

        if (at <= 1) {
            return "***" + email.substring(at);
        }

        return email.charAt(0)
                + "***"
                + email.substring(at);
    }
}