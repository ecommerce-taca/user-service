package com.ecommerce.authuser.auth.application;

import com.ecommerce.authuser.auth.exception.*;

import com.ecommerce.authuser.auth.security.OtpHasher;
import com.ecommerce.authuser.auth.security.SecureOtpGenerator;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.outbox.security.OutboxPayloadProtector;

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
public class PhoneOtpRequestService {

    private static final Duration OTP_TTL = Duration.ofMinutes(5);

    private static final Duration OTP_REQUEST_COOLDOWN = Duration.ofSeconds(60);

    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;

    private final VerificationTokenRepository verificationTokenRepository;

    private final IdentityNormalizer identityNormalizer;

    private final SecureOtpGenerator otpGenerator;

    private final OtpHasher otpHasher;

    private final OutboxEventRepository outboxEventRepository;

    private final OutboxPayloadProtector outboxPayloadProtector;

    @Transactional
    public PhoneOtpRequestResult request(PhoneOtpRequestCommand command) {
        Instant now = Instant.now();

        String phoneNormalized = identityNormalizer
                .normalizePhone(command.phone());

        if (phoneNormalized == null) {
            throw new InvalidPhoneFormatException();
        }

        User user = userRepository
                .findByIdForUpdate(command.userId())
                .orElseThrow(AccountSuspendedException::new);

        if (user.getStatus() == UserStatus.SUSPENDED
                || user.getStatus() == UserStatus.DELETED) {
            throw new AccountSuspendedException();
        }

        if (user.getPhoneVerifiedAt() != null
                && phoneNormalized.equals(user.getPhoneNormalized())
        ) {
            throw new VerificationAlreadyCompleteException();
        }

        boolean usedByAnotherUser = userRepository
                .existsByPhoneNormalizedAndIdNotAndDeletedAtIsNull(
                        phoneNormalized,
                        user.getId()
                );

        if (usedByAnotherUser) {
            throw new PhoneAlreadyExistsException();
        }

        long recentRequestCount = verificationTokenRepository
                .countByUser_IdAndPurposeAndChannelAndCreatedAtAfter(
                        user.getId(),
                        VerificationPurpose.PHONE_VERIFY,
                        VerificationChannel.PHONE,
                        now.minus(OTP_REQUEST_COOLDOWN)
                );

        if (recentRequestCount > 0) {
            throw new OtpRateLimitedException();
        }

        List<VerificationToken> activeChallenges = verificationTokenRepository
                .findActiveForUpdate(
                        user.getId(),
                        VerificationPurpose.PHONE_VERIFY,
                        VerificationChannel.PHONE
                );

        activeChallenges.forEach(token -> token.revoke(now));

        String rawOtp = otpGenerator.generate();

        String otpHash = otpHasher.hash(rawOtp);

        Instant expiresAt = now.plus(OTP_TTL);

        String maskedPhone = maskPhone(phoneNormalized);

        VerificationToken challenge = VerificationToken
                .createPhoneChallenge(
                        user,
                        otpHash,
                        phoneNormalized,
                        maskedPhone,
                        expiresAt
                );

        verificationTokenRepository.save(challenge);

        OutboxEvent notificationCommand =
                OutboxEvent.create(
                        OutboxAggregateType.USER,

                        user.getId(),

                        "PHONE_OTP_REQUESTED",

                        (short) 1,

                        user.getId()
                                .toString(),

                        outboxPayloadProtector.protect(
                                "PHONE_OTP_REQUESTED",

                                Map.of(
                                        "command_type",
                                        "PHONE_OTP_REQUESTED",

                                        "user_id",
                                        user.getId()
                                                .toString(),

                                        "channel",
                                        "SMS",

                                        "recipient",
                                        phoneNormalized,

                                        "template",
                                        "auth-phone-otp-v1",

                                        "dedupe_key",
                                        "phone-otp:"
                                                + challenge
                                                .getId(),

                                        "data",
                                        Map.of(
                                                "challenge_id",
                                                challenge
                                                        .getId()
                                                        .toString(),

                                                "otp",
                                                rawOtp,

                                                "expires_at",
                                                expiresAt
                                                        .toString()
                                        )
                                )
                        )
                );

        outboxEventRepository.save(notificationCommand);

        return new PhoneOtpRequestResult(
                challenge.getId(),
                maskedPhone,
                expiresAt,
                MAX_ATTEMPTS
        );
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 6) {
            return "***";
        }

        int maskedLength = phone.length() - 6;

        return phone.substring(0, 3)
                + "*".repeat(maskedLength)
                + phone.substring(
                phone.length() - 3
        );
    }
}