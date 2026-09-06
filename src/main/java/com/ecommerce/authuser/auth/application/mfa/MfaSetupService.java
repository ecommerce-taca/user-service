package com.ecommerce.authuser.auth.application.mfa;

import com.ecommerce.authuser.auth.exception.AccountSuspendedException;

import com.ecommerce.authuser.auth.exception.mfa.MfaAlreadyEnabledException;
import com.ecommerce.authuser.auth.exception.mfa.MfaSetupForbiddenException;

import com.ecommerce.authuser.mfa.domain.MfaChallenge;
import com.ecommerce.authuser.mfa.domain.MfaPurpose;

import com.ecommerce.authuser.mfa.domain.TwoFactorCredential;
import com.ecommerce.authuser.mfa.domain.TwoFactorStatus;

import com.ecommerce.authuser.mfa.repository.MfaChallengeRepository;
import com.ecommerce.authuser.mfa.repository.TwoFactorCredentialRepository;

import com.ecommerce.authuser.mfa.security.Base32Encoder;
import com.ecommerce.authuser.mfa.security.TotpSecretGenerator;
import com.ecommerce.authuser.mfa.security.TotpSecretProtector;

import com.ecommerce.authuser.rbac.domain.UserRole;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.domain.UserStatus;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.time.Duration;
import java.time.Instant;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MfaSetupService {

    private static final String ISSUER = "Taca Marketplace";

    private static final Duration SETUP_TTL = Duration.ofMinutes(5);

    private static final Set<String> ADMIN_ROLES =
            Set.of(
                    "SUPER_ADMIN",
                    "RISK_MANAGER",
                    "CATALOG_ADMIN",
                    "FINANCE_OPS",
                    "SUPPORT_VIEWER"
            );

    private final UserRepository userRepository;

    private final UserRoleRepository userRoleRepository;

    private final TwoFactorCredentialRepository credentialRepository;

    private final MfaChallengeRepository challengeRepository;

    private final TotpSecretGenerator secretGenerator;

    private final TotpSecretProtector secretProtector;

    private final Base32Encoder base32Encoder;

    @Transactional
    public MfaSetupResult setup(MfaSetupCommand command) {
        if (command == null || command.userId() == null) {
            throw new MfaSetupForbiddenException();
        }

        Instant now = Instant.now();

        User user = userRepository
                .findByIdForUpdate(command.userId())
                .orElseThrow(MfaSetupForbiddenException::new);

        if (user.getStatus() == UserStatus.SUSPENDED
                || user.getStatus() == UserStatus.DELETED) {
            throw new AccountSuspendedException();
        }

        if (!isAdmin(user.getId())) {
            throw new MfaSetupForbiddenException();
        }

        TwoFactorCredential credential = credentialRepository
                .findByUserIdForUpdate(user.getId())
                .orElse(null);

        if (credential != null
                && credential.getStatus() == TwoFactorStatus.ENABLED) {
            throw new MfaAlreadyEnabledException();
        }

        byte[] rawSecret = secretGenerator.generate();

        byte[] encryptedSecret =
                secretProtector.encrypt(
                        user.getId(),
                        rawSecret
                );

        if (credential == null) {

            credential = TwoFactorCredential.createEnrollment(
                    user,
                    encryptedSecret,
                    secretProtector.keyVersion()
            );

            credentialRepository.save(credential);
        } else if (credential.getStatus() == TwoFactorStatus.RESET_REQUIRED) {
            credential.beginResetEnrollment(
                    encryptedSecret,
                    secretProtector.keyVersion()
            );
        } else {
            credential.beginEnrollment(
                    encryptedSecret,
                    secretProtector.keyVersion()
            );
        }

        List<MfaChallenge> activeSetups =
                challengeRepository
                        .findActiveForUpdate(
                                user.getId(),
                                MfaPurpose.ENROLL,
                                now
                        );

        activeSetups.forEach(
                challenge ->
                        challenge.revoke(now)
        );

        Instant expiresAt = now.plus(SETUP_TTL);

        MfaChallenge setupChallenge =
                MfaChallenge.create(
                        user,
                        MfaPurpose.ENROLL,
                        null,
                        now,
                        expiresAt
                );

        challengeRepository.save(setupChallenge);

        String base32Secret = base32Encoder.encode(rawSecret);

        String otpauthUri =
                buildOtpAuthUri(
                        user.getEmail(),
                        base32Secret
                );

        java.util.Arrays.fill(rawSecret, (byte) 0);

        return new MfaSetupResult(
                setupChallenge.getId(),
                ISSUER,
                user.getEmail(),
                otpauthUri,
                expiresAt
        );
    }

    private boolean isAdmin(java.util.UUID userId) {

        return userRoleRepository
                .findAllByUser_IdAndRevokedAtIsNull(userId)
                .stream()
                .map(UserRole::getRole)
                .map(role -> role.getRoleKey())
                .anyMatch(ADMIN_ROLES::contains);
    }

    private String buildOtpAuthUri(
            String account,
            String secret
    ) {

        String label = urlEncode(ISSUER) + ":" + urlEncode(account);

        return "otpauth://totp/"
                + label
                + "?secret="
                + secret
                + "&issuer="
                + urlEncode(ISSUER)
                + "&algorithm=SHA1"
                + "&digits=6"
                + "&period=30";
    }

    private String urlEncode(String value) {
        return URLEncoder
                .encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
