package com.ecommerce.authuser.auth.application.mfa;

import com.ecommerce.authuser.auth.exception.AccountSuspendedException;
import com.ecommerce.authuser.auth.exception.mfa.InvalidMfaCodeException;
import com.ecommerce.authuser.auth.exception.mfa.InvalidMfaVerifyRequestException;
import com.ecommerce.authuser.auth.exception.mfa.MfaAttemptsExceededException;
import com.ecommerce.authuser.auth.exception.mfa.MfaChallengeExpiredException;

import com.ecommerce.authuser.auth.security.SecureTokenGenerator;
import com.ecommerce.authuser.auth.security.TokenHasher;

import com.ecommerce.authuser.mfa.domain.MfaChallenge;
import com.ecommerce.authuser.mfa.domain.MfaMethod;
import com.ecommerce.authuser.mfa.domain.MfaPurpose;
import com.ecommerce.authuser.mfa.domain.MfaStepUpToken;
import com.ecommerce.authuser.mfa.domain.TwoFactorCredential;
import com.ecommerce.authuser.mfa.domain.TwoFactorStatus;

import com.ecommerce.authuser.mfa.repository.MfaChallengeRepository;
import com.ecommerce.authuser.mfa.repository.MfaStepUpTokenRepository;
import com.ecommerce.authuser.mfa.repository.TwoFactorCredentialRepository;
import com.ecommerce.authuser.mfa.repository.TwoFactorRecoveryCodeRepository;

import com.ecommerce.authuser.mfa.security.TotpSecretProtector;
import com.ecommerce.authuser.mfa.security.TotpVerificationResult;
import com.ecommerce.authuser.mfa.security.TotpVerifier;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.domain.UserStatus;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MfaStepUpVerifyService {

    private static final int MAX_ATTEMPTS = 5;

    private static final Duration STEP_UP_TOKEN_TTL = Duration.ofMinutes(5);

    private final UserRepository userRepository;

    private final TwoFactorCredentialRepository credentialRepository;

    private final MfaChallengeRepository challengeRepository;

    private final TwoFactorRecoveryCodeRepository recoveryCodeRepository;

    private final MfaStepUpTokenRepository stepUpTokenRepository;

    private final TotpSecretProtector secretProtector;

    private final TotpVerifier totpVerifier;

    private final SecureTokenGenerator tokenGenerator;

    private final TokenHasher tokenHasher;

    @Transactional(
            noRollbackFor = {
                    InvalidMfaCodeException.class,
                    MfaAttemptsExceededException.class
            }
    )
    public MfaStepUpVerifyResult verify(MfaStepUpVerifyCommand command) {
        validateCommand(command);

        Instant now = Instant.now();

        User user = userRepository
                .findByIdForUpdate(command.userId())
                .orElseThrow(MfaChallengeExpiredException::new);

        if (user.getStatus() == UserStatus.SUSPENDED
                || user.getStatus() == UserStatus.DELETED) {
            throw new AccountSuspendedException();
        }

        TwoFactorCredential credential = credentialRepository
                .findByUserIdForUpdate(user.getId())
                .orElseThrow(MfaChallengeExpiredException::new);

        if (credential.getStatus() != TwoFactorStatus.ENABLED) {
            throw new MfaChallengeExpiredException();
        }

        MfaChallenge challenge = challengeRepository
                .findForVerification(
                        command.challengeId(),
                        user.getId(),
                        MfaPurpose.STEP_UP
                )
                .orElseThrow(MfaChallengeExpiredException::new);

        if (!challenge.belongsToSession(command.sessionId())) {
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

        boolean valid =
                verifyFactor(
                        credential,
                        user.getId(),
                        command.method(),
                        command.code(),
                        now
                );

        if (!valid) {
            challenge.recordFailedAttempt(MAX_ATTEMPTS);

            if (challenge.getAttemptCount() >= MAX_ATTEMPTS) {
                challenge.revoke(now);

                throw new MfaAttemptsExceededException();
            }

            throw new InvalidMfaCodeException();
        }

        challenge.markVerified(now, MAX_ATTEMPTS);

        String rawStepUpToken = tokenGenerator.generate();

        String tokenHash = tokenHasher.hash(rawStepUpToken);

        Instant tokenExpiresAt = now.plus(STEP_UP_TOKEN_TTL);

        MfaStepUpToken stepUpToken =
                MfaStepUpToken.issue(
                        user,
                        command.sessionId(),
                        challenge,
                        tokenHash,
                        now,
                        tokenExpiresAt
                );

        stepUpTokenRepository.save(stepUpToken);

        return new MfaStepUpVerifyResult(
                rawStepUpToken,
                tokenExpiresAt
        );
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

    private boolean verifyTotp(
            TwoFactorCredential credential,
            UUID userId,
            String code,
            Instant now
    ) {

        if (code == null || !code.matches("^\\d{6}$")) {
            return false;
        }

        byte[] rawSecret =
                secretProtector.decrypt(
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

    private boolean verifyRecoveryCode(
            TwoFactorCredential credential,
            String code,
            Instant now
    ) {

        if (code == null
                || code.isBlank()
                || code.length() > 128) {

            return false;
        }

        String codeHash = tokenHasher.hash(code);

        return recoveryCodeRepository
                .findUsableCodeForUpdate(
                        credential.getId(),
                        codeHash
                )
                .map(recoveryCode -> {
                    recoveryCode.markUsed(now);
                    return true;
                })
                .orElse(false);
    }

    private void validateCommand(
            MfaStepUpVerifyCommand command
    ) {

        if (command == null
                || command.userId() == null
                || command.sessionId() == null
                || command.challengeId() == null
                || command.method() == null) {

            throw new InvalidMfaVerifyRequestException();
        }
    }
}
