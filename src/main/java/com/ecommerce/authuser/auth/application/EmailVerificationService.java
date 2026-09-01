package com.ecommerce.authuser.auth.application;

import com.ecommerce.authuser.auth.exception.InvalidVerificationTokenException;
import com.ecommerce.authuser.auth.security.TokenHasher;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.outbox.security.OutboxPayloadProtector;

import com.ecommerce.authuser.token.domain.VerificationChannel;
import com.ecommerce.authuser.token.domain.VerificationPurpose;
import com.ecommerce.authuser.token.domain.VerificationToken;
import com.ecommerce.authuser.token.repository.VerificationTokenLookup;
import com.ecommerce.authuser.token.repository.VerificationTokenRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final VerificationTokenRepository verificationTokenRepository;

    private final UserRepository userRepository;

    private final TokenHasher tokenHasher;

    private final OutboxEventRepository outboxEventRepository;

    private final OutboxPayloadProtector outboxPayloadProtector;

    @Transactional
    public EmailVerificationResult verify(EmailVerificationCommand command) {
        Instant now = Instant.now();

        String tokenHash = tokenHasher.hash(command.token());

        VerificationTokenLookup lookup =
                verificationTokenRepository
                        .findLookup(
                                tokenHash,
                                VerificationChannel.EMAIL,
                                VerificationPurpose.EMAIL_VERIFY
                        )
                        .orElseThrow(InvalidVerificationTokenException::new);

        User user = userRepository
                .findByIdForUpdate(lookup.userId())
                .orElseThrow(InvalidVerificationTokenException::new);

        VerificationToken verificationToken =
                verificationTokenRepository
                        .findByIdForUpdate(lookup.tokenId())
                        .orElseThrow(InvalidVerificationTokenException::new);

        if (!verificationToken
                .getUser()
                .getId()
                .equals(user.getId())) {

            throw new InvalidVerificationTokenException();
        }

        if (!verificationToken.isUsable(now)) {
            throw new InvalidVerificationTokenException();
        }

        if (user.getEmailVerifiedAt() != null) {
            throw new InvalidVerificationTokenException();
        }

        user.verifyEmail(now);

        verificationToken.markUsed(now);

        OutboxEvent event =
                OutboxEvent.create(
                        OutboxAggregateType.USER,
                        user.getId(),

                        "user.email_verified",

                        (short) 1,

                        user.getId().toString(),

                        outboxPayloadProtector.protect(
                                "user.email_verified",

                                Map.of(
                                        "user_id", user.getId().toString(),

                                        "email_verified_at", now.toString()
                                )
                        )
                );

        outboxEventRepository.save(event);

        return new EmailVerificationResult(
                user.getId(),
                now
        );
    }
}