package com.ecommerce.authuser.auth.application.mfa;

import com.ecommerce.authuser.auth.exception.AccountSuspendedException;
import com.ecommerce.authuser.auth.exception.mfa.InvalidMfaCodeException;
import com.ecommerce.authuser.auth.exception.mfa.InvalidMfaVerifyRequestException;
import com.ecommerce.authuser.auth.exception.mfa.MfaAttemptsExceededException;
import com.ecommerce.authuser.auth.exception.mfa.MfaChallengeExpiredException;

import com.ecommerce.authuser.auth.security.AccessTokenService;
import com.ecommerce.authuser.auth.security.SecureTokenGenerator;
import com.ecommerce.authuser.auth.security.TokenHasher;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.mfa.domain.MfaChallenge;
import com.ecommerce.authuser.mfa.domain.MfaMethod;
import com.ecommerce.authuser.mfa.domain.MfaPurpose;
import com.ecommerce.authuser.mfa.domain.TwoFactorCredential;
import com.ecommerce.authuser.mfa.domain.TwoFactorStatus;

import com.ecommerce.authuser.mfa.repository.MfaChallengeLookup;
import com.ecommerce.authuser.mfa.repository.MfaChallengeRepository;
import com.ecommerce.authuser.mfa.repository.TwoFactorCredentialRepository;

import com.ecommerce.authuser.mfa.repository.TwoFactorRecoveryCodeRepository;
import com.ecommerce.authuser.mfa.security.TotpSecretProtector;
import com.ecommerce.authuser.mfa.security.TotpVerificationResult;
import com.ecommerce.authuser.mfa.security.TotpVerifier;

import com.ecommerce.authuser.rbac.domain.UserRole;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import com.ecommerce.authuser.security.domain.LoginAttempt;
import com.ecommerce.authuser.security.repository.LoginAttemptRepository;
import com.ecommerce.authuser.token.domain.RefreshToken;
import com.ecommerce.authuser.token.repository.RefreshTokenRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.domain.UserStatus;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MfaLoginVerifyService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;

    private final UserRoleRepository userRoleRepository;

    private final MfaChallengeRepository challengeRepository;

    private final TwoFactorCredentialRepository credentialRepository;

    private final TotpSecretProtector secretProtector;

    private final TotpVerifier totpVerifier;

    private final SecureTokenGenerator tokenGenerator;

    private final TokenHasher tokenHasher;

    private final AccessTokenService accessTokenService;

    private final RefreshTokenRepository refreshTokenRepository;

    private final TwoFactorRecoveryCodeRepository recoveryCodeRepository;

    private final LoginAttemptRepository loginAttemptRepository;

    @Transactional(
            noRollbackFor = {
                    InvalidMfaCodeException.class,
                    MfaAttemptsExceededException.class
            }
    )
    public MfaLoginVerifyResult verify(MfaLoginVerifyCommand command) {
        validateCommand(command);

        Instant now = Instant.now();

        MfaChallengeLookup lookup =
                challengeRepository
                        .findLookupById(command.challengeId())
                        .orElseThrow(MfaChallengeExpiredException::new);

        if (lookup.purpose() != MfaPurpose.LOGIN) {
            throw new MfaChallengeExpiredException();
        }

        User user = userRepository
                .findByIdForUpdate(lookup.userId())
                .orElseThrow(MfaChallengeExpiredException::new);

        if (user.getStatus() == UserStatus.SUSPENDED
                || user.getStatus() == UserStatus.DELETED) {
            throw new AccountSuspendedException();
        }

        TwoFactorCredential credential = credentialRepository
                .findByUserIdForUpdate(user.getId())
                .orElseThrow(MfaChallengeExpiredException::new);

        TwoFactorStatus credentialStatus = credential.getStatus();

        if (credentialStatus != TwoFactorStatus.ENABLED
                && credentialStatus != TwoFactorStatus.RESET_REQUIRED) {
            throw new MfaChallengeExpiredException();
        }

        MfaChallenge challenge = challengeRepository
                .findForVerification(
                        command.challengeId(),
                        user.getId(),
                        MfaPurpose.LOGIN
                )
                .orElseThrow(MfaChallengeExpiredException::new);

        if (!challenge.hasLoginAuditContext()) {
            throw new MfaChallengeExpiredException();
        }

        if (challenge.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new MfaAttemptsExceededException();
        }

        if (challenge.isExpired(now)
                || challenge.isRevoked()
                || challenge.isVerified()) {
            throw new MfaChallengeExpiredException();
        }

        boolean valid;

        if (credentialStatus == TwoFactorStatus.RESET_REQUIRED) {
            valid = command.method() == MfaMethod.RECOVERY_CODE
                    && verifyRecoveryCode(credential, command.code(), now
            );
        } else {
            valid = verifyFactor(
                    credential,
                    user.getId(),
                    command.method(),
                    command.code(),
                    now
            );
        }

        if (!valid) {
            challenge.recordFailedAttempt(MAX_ATTEMPTS);

            if (challenge.getAttemptCount() >= MAX_ATTEMPTS) {
                challenge.revoke(now);
                throw new MfaAttemptsExceededException();
            }

            throw new InvalidMfaCodeException();
        }

        challenge.markVerified(now, MAX_ATTEMPTS);

        List<String> roles = loadRoles(user.getId());

        String rawRefreshToken = tokenGenerator.generate();

        String refreshTokenHash = tokenHasher.hash(rawRefreshToken);

        UUID familyId = UuidV7Generator.generate();

        RefreshToken refreshToken =
                RefreshToken.issue(
                        user,
                        refreshTokenHash,
                        familyId,
                        now,
                        now.plus(REFRESH_TOKEN_TTL)
                );

        refreshTokenRepository.save(refreshToken);

        boolean emailVerified = user.getEmailVerifiedAt() != null;

        String accessToken = accessTokenService.issue(
                user.getId(),
                familyId,
                roles,
                emailVerified,
                now,
                now.plus(ACCESS_TOKEN_TTL)
        );

        loginAttemptRepository.save(
                LoginAttempt.success(
                        user,
                        challenge.getLoginIdentifierHash(),
                        challenge.getLoginIpHash(),
                        challenge.getLoginUserAgentHash(),
                        now
                )
        );

        return new MfaLoginVerifyResult(
                accessToken,
                rawRefreshToken,
                ACCESS_TOKEN_TTL.toSeconds(),
                REFRESH_TOKEN_TTL.toSeconds()
        );
    }

    private boolean verifyTotp(
            TwoFactorCredential credential,
            UUID userId,
            String code,
            Instant now
    ) {

        if (code == null || !code.matches("^\\d{6}$")) {
            return false;
        }

        byte[] rawSecret = secretProtector.decrypt(
                userId,
                credential.getSecretCiphertextCopy(),
                credential.getKeyVersion()
        );

        try {
            TotpVerificationResult verification =
                    totpVerifier.verifyWithStep(
                            rawSecret,
                            code,
                            now
                    );

            if (!verification.valid()) {
                return false;
            }

            Long matchedStep = verification.matchedStep();

            if (matchedStep == null) {
                return false;
            }

            if (credential.hasUsedTotpStep(matchedStep)) {
                return false;
            }

            credential.recordTotpStepUsed(matchedStep);

            return true;

        } finally {
            Arrays.fill(rawSecret, (byte) 0);
        }
    }

    private List<String> loadRoles(UUID userId) {

        return userRoleRepository
                .findAllByUser_IdAndRevokedAtIsNull(userId)
                .stream()
                .map(UserRole::getRole)
                .map(role -> role.getRoleKey())
                .distinct()
                .sorted()
                .toList();
    }

    private void validateCommand(MfaLoginVerifyCommand command) {

        if (command == null || command.challengeId() == null) {
            throw new MfaChallengeExpiredException();
        }

        if (command.method() == null) {
            throw new InvalidMfaVerifyRequestException();
        }
    }

    private boolean verifyFactor(
            TwoFactorCredential credential,
            UUID userId,
            MfaMethod method,
            String code,
            Instant now
    ) {

        return switch (method) {
            case TOTP ->
                    verifyTotp(
                            credential,
                            userId,
                            code,
                            now
                    );

            case RECOVERY_CODE ->
                    verifyRecoveryCode(
                            credential,
                            code,
                            now
                    );
        };
    }

    private boolean verifyRecoveryCode(
            TwoFactorCredential credential,
            String code,
            Instant now
    ) {
        if (code == null || code.isBlank() || code.length() > 128) {
            return false;
        }

        String codeHash = tokenHasher.hash(code);

        return recoveryCodeRepository
                .findUsableCodeForUpdate(credential.getId(), codeHash)
                .map(recoveryCode -> {
                    recoveryCode.markUsed(now);
                    return true;
                })
                .orElse(false);
    }
}
