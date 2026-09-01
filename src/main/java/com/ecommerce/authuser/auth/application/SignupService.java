package com.ecommerce.authuser.auth.application;

import com.ecommerce.authuser.auth.exception.EmailAlreadyExistsException;
import com.ecommerce.authuser.auth.exception.PhoneAlreadyExistsException;
import com.ecommerce.authuser.auth.security.AccessTokenService;
import com.ecommerce.authuser.auth.security.PasswordHasher;
import com.ecommerce.authuser.auth.security.SecureTokenGenerator;
import com.ecommerce.authuser.auth.security.TokenHasher;
import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.outbox.security.OutboxPayloadProtector;
import com.ecommerce.authuser.rbac.domain.Role;
import com.ecommerce.authuser.rbac.domain.UserRole;
import com.ecommerce.authuser.rbac.repository.RoleRepository;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;
import com.ecommerce.authuser.token.domain.RefreshToken;
import com.ecommerce.authuser.token.domain.VerificationChannel;
import com.ecommerce.authuser.token.domain.VerificationPurpose;
import com.ecommerce.authuser.token.domain.VerificationToken;
import com.ecommerce.authuser.token.repository.RefreshTokenRepository;
import com.ecommerce.authuser.token.repository.VerificationTokenRepository;
import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignupService {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private static final Duration EMAIL_VERIFICATION_TTL = Duration.ofHours(24);

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserRoleRepository userRoleRepository;

    private final VerificationTokenRepository verificationTokenRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final IdentityNormalizer identityNormalizer;

    private final PasswordHasher passwordHasher;

    private final SecureTokenGenerator tokenGenerator;

    private final TokenHasher tokenHasher;

    private final AccessTokenService accessTokenService;

    private final OutboxEventRepository outboxEventRepository;

    private final OutboxPayloadProtector outboxPayloadProtector;

    @Transactional
    public SignupResult signup(SignupCommand command) {
        Instant now = Instant.now();

        String fullName = command.fullName().trim();

        String email = command.email().trim();

        String emailNormalized = identityNormalizer.normalizeEmail(email);

        String phoneNormalized = identityNormalizer
                .normalizePhone(command.phone());

        if (userRepository.existsByEmailNormalized(emailNormalized)) {
            throw new EmailAlreadyExistsException();
        }

        if (phoneNormalized != null && userRepository.existsByPhoneNormalized(phoneNormalized)) {
            throw new PhoneAlreadyExistsException();
        }

        String passwordHash = passwordHasher.hash(command.password());

        User user = User.registerBuyer(
                email,
                emailNormalized,
                passwordHash,
                fullName,
                phoneNormalized,
                phoneNormalized
        );

        userRepository.save(user);

        Role buyerRole = roleRepository
                .findByRoleKey("BUYER")
                .orElseThrow(() -> new IllegalStateException("BUYER role is missing"));

        UserRole buyerAssignment = UserRole.assign(
                user,
                buyerRole,
                null,
                null
        );

        userRoleRepository.save(buyerAssignment);

        String rawVerificationToken = tokenGenerator.generate();

        String verificationHash = tokenHasher.hash(rawVerificationToken);

        Instant verificationExpiresAt = now.plus(EMAIL_VERIFICATION_TTL);

        VerificationToken verificationToken = VerificationToken.create(
                user,
                VerificationChannel.EMAIL,
                VerificationPurpose.EMAIL_VERIFY,
                verificationHash,
                maskEmail(emailNormalized),
                verificationExpiresAt
        );

        verificationTokenRepository.save(verificationToken);

        String rawRefreshToken = tokenGenerator.generate();

        String refreshHash = tokenHasher.hash(rawRefreshToken);

        UUID familyId = UuidV7Generator.generate();

        RefreshToken refreshToken = RefreshToken.issue(
                user,
                refreshHash,
                familyId,
                now,
                now.plus(REFRESH_TOKEN_TTL)
        );

        refreshTokenRepository.save(refreshToken);

        OutboxEvent userCreatedEvent = OutboxEvent.create(
                OutboxAggregateType.USER,
                user.getId(),
                "user.created",
                (short) 1,
                user.getId().toString(),
                outboxPayloadProtector.protect(
                        "user.created",
                        Map.of(
                                "user_id", user.getId().toString(),

                                "email", user.getEmail(),

                                "status",
                                "ACTIVE",

                                "roles",
                                List.of("BUYER")
                        )
                )
        );

        outboxEventRepository.save(userCreatedEvent);

        OutboxEvent verificationEvent = OutboxEvent.create(
                OutboxAggregateType.USER,
                user.getId(),
                "AUTH_VERIFICATION_REQUESTED",
                (short) 1,
                user.getId().toString(),
                outboxPayloadProtector.protect(
                        "AUTH_VERIFICATION_REQUESTED",
                        Map.of(
                                "command_type",
                                "AUTH_VERIFICATION_REQUESTED",

                                "user_id", user.getId().toString(),

                                "channel",
                                "EMAIL",

                                "recipient", user.getEmail(),

                                "display_name", user.getFullName(),

                                "verification_token", rawVerificationToken,

                                "expires_at", verificationExpiresAt.toString()
                        )
                )
        );

        outboxEventRepository.save(verificationEvent);

        Instant accessExpiresAt = now.plus(ACCESS_TOKEN_TTL);

        String accessToken = accessTokenService.issue(
                user.getId(),
                familyId,
                List.of("BUYER"),
                false,
                now,
                accessExpiresAt
        );

        return new SignupResult(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                accessToken,
                rawRefreshToken,
                ACCESS_TOKEN_TTL.toSeconds(),
                REFRESH_TOKEN_TTL.toSeconds(),
                verificationExpiresAt
        );
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
