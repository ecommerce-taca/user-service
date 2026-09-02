package com.ecommerce.authuser.auth.application;

import com.ecommerce.authuser.auth.exception.AccountSuspendedException;
import com.ecommerce.authuser.auth.exception.InvalidOtpException;
import com.ecommerce.authuser.auth.exception.InvalidVerificationTokenException;
import com.ecommerce.authuser.auth.exception.OtpAttemptsExceededException;
import com.ecommerce.authuser.auth.exception.PhoneAlreadyExistsException;

import com.ecommerce.authuser.auth.security.OtpHasher;

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

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PhoneOtpVerifyService {

    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;

    private final VerificationTokenRepository verificationTokenRepository;

    private final OtpHasher otpHasher;

    @Transactional(
            noRollbackFor = {
                    InvalidOtpException.class,
                    OtpAttemptsExceededException.class
            }
    )
    public PhoneOtpVerifyResult verify(PhoneOtpVerifyCommand command) {
        Instant now = Instant.now();

        User user = userRepository
                .findByIdForUpdate(command.userId())
                .orElseThrow(AccountSuspendedException::new);

        if (user.getStatus() == UserStatus.SUSPENDED
                || user.getStatus() == UserStatus.DELETED) {

            throw new AccountSuspendedException();
        }

        VerificationToken challenge = verificationTokenRepository
                .findChallengeForUpdate(
                        command.challengeId(),
                        user.getId(),
                        VerificationChannel.PHONE,
                        VerificationPurpose.PHONE_VERIFY
                )
                .orElseThrow(InvalidVerificationTokenException::new);

        if (!challenge.isUsable(now)) {
            throw new InvalidVerificationTokenException();
        }

        if (command.otp() == null
                || !command.otp().matches("\\d{6}")) {
            throw new InvalidVerificationTokenException();
        }

        String candidateHash = otpHasher.hash(command.otp());

        boolean otpMatches = challenge.matchesTokenHash(candidateHash);

        if (!otpMatches) {
            challenge.recordFailedAttempt(MAX_ATTEMPTS);

            if (challenge.getAttemptCount() >= MAX_ATTEMPTS) {
                challenge.revoke(now);

                throw new OtpAttemptsExceededException();
            }

            throw new InvalidOtpException();
        }

        String verifiedPhone = challenge.getRecipientValue();

        if (verifiedPhone == null || verifiedPhone.isBlank()) {
            throw new InvalidVerificationTokenException();
        }

        boolean usedByAnotherUser = userRepository
                .existsByPhoneNormalizedAndIdNotAndDeletedAtIsNull(
                        verifiedPhone,
                        user.getId()
                );

        if (usedByAnotherUser) {
            throw new PhoneAlreadyExistsException();
        }

        user.verifyPhone(
                verifiedPhone,
                verifiedPhone,
                now
        );

        challenge.markUsed(now);

        return new PhoneOtpVerifyResult(now);
    }
}
