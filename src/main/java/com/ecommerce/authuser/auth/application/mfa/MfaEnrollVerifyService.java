package com.ecommerce.authuser.auth.application.mfa;

import com.ecommerce.authuser.auth.exception.mfa.InvalidMfaCodeException;
import com.ecommerce.authuser.auth.exception.mfa.MfaAttemptsExceededException;
import com.ecommerce.authuser.auth.exception.mfa.MfaChallengeExpiredException;

import com.ecommerce.authuser.auth.security.TokenHasher;

import com.ecommerce.authuser.mfa.domain.MfaChallenge;
import com.ecommerce.authuser.mfa.domain.MfaPurpose;
import com.ecommerce.authuser.mfa.domain.TwoFactorCredential;
import com.ecommerce.authuser.mfa.domain.TwoFactorRecoveryCode;
import com.ecommerce.authuser.mfa.domain.TwoFactorStatus;

import com.ecommerce.authuser.mfa.repository.MfaChallengeRepository;
import com.ecommerce.authuser.mfa.repository.TwoFactorCredentialRepository;
import com.ecommerce.authuser.mfa.repository.TwoFactorRecoveryCodeRepository;

import com.ecommerce.authuser.mfa.security.RecoveryCodeGenerator;
import com.ecommerce.authuser.mfa.security.TotpSecretProtector;
import com.ecommerce.authuser.mfa.security.TotpVerificationResult;
import com.ecommerce.authuser.mfa.security.TotpVerifier;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MfaEnrollVerifyService {

    private static final int MAX_ATTEMPTS = 5;

    private static final int RECOVERY_CODE_COUNT = 10;

    private final UserRepository userRepository;

    private final TwoFactorCredentialRepository credentialRepository;

    private final MfaChallengeRepository challengeRepository;

    private final TwoFactorRecoveryCodeRepository recoveryCodeRepository;

    private final TotpSecretProtector secretProtector;

    private final TotpVerifier totpVerifier;

    private final RecoveryCodeGenerator recoveryCodeGenerator;

    private final TokenHasher tokenHasher;

    @Transactional(
            noRollbackFor = {
                    InvalidMfaCodeException.class,
                    MfaAttemptsExceededException.class
            }
    )
    public MfaEnrollVerifyResult verify(MfaEnrollVerifyCommand command) {
        validateCommand(command);

        Instant now = Instant.now();

        User user = userRepository
                .findByIdForUpdate(command.userId())
                .orElseThrow(MfaChallengeExpiredException::new);

        TwoFactorCredential credential = credentialRepository
                .findByUserIdForUpdate(user.getId())
                .orElseThrow(MfaChallengeExpiredException::new);

        TwoFactorStatus credentialStatus = credential.getStatus();

        if (credentialStatus != TwoFactorStatus.ENROLLING
                && credentialStatus != TwoFactorStatus.RESET_REQUIRED) {
            throw new MfaChallengeExpiredException();
        }

        MfaChallenge challenge = challengeRepository
                .findForVerification(
                        command.setupId(),
                        user.getId(),
                        MfaPurpose.ENROLL
                )
                .orElseThrow(MfaChallengeExpiredException::new);

        if (challenge.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new MfaAttemptsExceededException();
        }

        if (challenge.isExpired(now)
                || challenge.isRevoked()
                || challenge.isVerified()) {

            throw new MfaChallengeExpiredException();
        }

        byte[] rawSecret = secretProtector.decrypt(
                user.getId(),
                credential.getSecretCiphertextCopy(),
                credential.getKeyVersion()
        );

        TotpVerificationResult verification;

        try {
            verification =
                    totpVerifier.verifyWithStep(
                            rawSecret,
                            command.code(),
                            now
                    );

        } finally {
            Arrays.fill(rawSecret, (byte) 0);
        }

        boolean valid =
                verification.valid()
                        && verification.matchedStep() != null
                        && !credential.hasUsedTotpStep(
                        verification.matchedStep()
                );

        if (!valid) {
            challenge.recordFailedAttempt(MAX_ATTEMPTS);

            if (challenge.getAttemptCount() >= MAX_ATTEMPTS) {
                challenge.revoke(now);

                throw new MfaAttemptsExceededException();
            }

            throw new InvalidMfaCodeException();
        }

        long matchedStep = verification.matchedStep();

        credential.recordTotpStepUsed(matchedStep);

        credential.enable(now);

        challenge.markVerified(now, MAX_ATTEMPTS);

        recoveryCodeRepository.deleteAllByCredentialId(credential.getId());

        List<String> rawRecoveryCodes = new ArrayList<>(RECOVERY_CODE_COUNT);

        List<TwoFactorRecoveryCode> recoveryCodeEntities = new ArrayList<>(RECOVERY_CODE_COUNT);

        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String rawCode = recoveryCodeGenerator.generate();

            String codeHash = tokenHasher.hash(rawCode);

            rawRecoveryCodes.add(rawCode);

            recoveryCodeEntities.add(TwoFactorRecoveryCode
                    .create(credential, codeHash)
            );
        }

        recoveryCodeRepository.saveAll(recoveryCodeEntities);

        return new MfaEnrollVerifyResult(
                "ENABLED",
                now,
                rawRecoveryCodes
        );
    }

    private void validateCommand(
            MfaEnrollVerifyCommand command
    ) {

        if (command == null
                || command.userId() == null
                || command.setupId() == null) {

            throw new MfaChallengeExpiredException();
        }

        if (command.code() == null
                || !command.code()
                .matches("^\\d{6}$")) {

            throw new InvalidMfaCodeException();
        }
    }
}
