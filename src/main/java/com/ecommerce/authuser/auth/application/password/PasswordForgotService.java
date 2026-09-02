package com.ecommerce.authuser.auth.application.password;

import com.ecommerce.authuser.auth.application.IdentityNormalizer;
import com.ecommerce.authuser.auth.exception.InvalidPhoneFormatException;
import com.ecommerce.authuser.auth.exception.password.InvalidPasswordRecoveryInputException;
import com.ecommerce.authuser.auth.security.SecureTokenGenerator;
import com.ecommerce.authuser.auth.security.TokenHasher;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.outbox.security.OutboxPayloadProtector;

import com.ecommerce.authuser.token.domain.PasswordResetToken;
import com.ecommerce.authuser.token.repository.PasswordResetTokenRepository;

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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordForgotService {

    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final IdentityNormalizer identityNormalizer;

    private final SecureTokenGenerator tokenGenerator;

    private final TokenHasher tokenHasher;

    private final OutboxEventRepository outboxEventRepository;

    private final OutboxPayloadProtector outboxPayloadProtector;

    @Transactional
    public PasswordForgotResult forgot(PasswordForgotCommand command) {
        String identifier =
                command.identifier() == null
                        ? null
                        : command.identifier().trim();

        if (identifier == null || identifier.isBlank()) {
            throw new InvalidPasswordRecoveryInputException();
        }

        Optional<User> userOptional = resolveAndLockUser(identifier);


        if (userOptional.isEmpty()) {

            return PasswordForgotResult.success();
        }

        User user = userOptional.get();

        if (user.getStatus()
                == UserStatus.SUSPENDED
                || user.getStatus()
                == UserStatus.DELETED) {

            return PasswordForgotResult.success();
        }

        boolean phoneIdentifier =
                isPhoneIdentifier(
                        identifier
                );

        if (phoneIdentifier && user.getPhoneVerifiedAt() == null) {
            return PasswordForgotResult.success();
        }

        Instant now = Instant.now();

        List<PasswordResetToken> activeTokens = passwordResetTokenRepository
                .findActiveForUpdate(user.getId(), now);

        activeTokens.forEach(token -> token.revoke(now));

        String rawResetToken = tokenGenerator.generate();

        String tokenHash = tokenHasher.hash(rawResetToken);

        Instant expiresAt = now.plus(RESET_TOKEN_TTL);

        PasswordResetToken resetToken =
                PasswordResetToken.create(
                        user,
                        tokenHash,
                        expiresAt
                );

        passwordResetTokenRepository.save(resetToken);

        String channel = phoneIdentifier ? "SMS" : "EMAIL";

        String recipient = phoneIdentifier
                ? user.getPhoneNormalized()
                : user.getEmail();

        OutboxEvent notificationCommand =
                OutboxEvent.create(
                        OutboxAggregateType.USER,

                        user.getId(),

                        "PASSWORD_RESET_REQUESTED",

                        (short) 1,

                        user.getId().toString(),

                        outboxPayloadProtector.protect(
                                "PASSWORD_RESET_REQUESTED",

                                Map.of(
                                        "command_type",
                                        "PASSWORD_RESET_REQUESTED",

                                        "user_id",
                                        user.getId().toString(),

                                        "channel",
                                        channel,

                                        "recipient",
                                        recipient,

                                        "template",
                                        "auth-password-reset-v1",

                                        "dedupe_key",
                                        "password-reset:"
                                                + resetToken
                                                .getId(),

                                        "data",
                                        Map.of(
                                                "reset_token",
                                                rawResetToken,

                                                "expires_at",
                                                expiresAt
                                                        .toString()
                                        )
                                )
                        )
                );

        outboxEventRepository.save(notificationCommand);

        return PasswordForgotResult.success();
    }

    private Optional<User> resolveAndLockUser(String identifier) {
        if (identifier.contains("@")) {

            String emailNormalized = identityNormalizer
                    .normalizeEmail(identifier);

            return userRepository.findByEmailNormalizedForUpdate(emailNormalized);
        }

        String phoneNormalized;

        try {
            phoneNormalized = identityNormalizer.normalizePhone(identifier);

        } catch (InvalidPhoneFormatException ex) {
            throw ex;
        }

        if (phoneNormalized == null) {
            throw new InvalidPhoneFormatException();
        }

        return userRepository
                .findByPhoneNormalizedForUpdate(phoneNormalized);
    }

    private boolean isPhoneIdentifier(String identifier) {
        return !identifier.contains("@");
    }
}