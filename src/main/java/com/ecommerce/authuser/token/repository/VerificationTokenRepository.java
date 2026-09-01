package com.ecommerce.authuser.token.repository;

import com.ecommerce.authuser.token.domain.VerificationChannel;
import com.ecommerce.authuser.token.domain.VerificationPurpose;
import com.ecommerce.authuser.token.domain.VerificationToken;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByTokenHashAndChannelAndPurpose(
            String tokenHash,
            VerificationChannel channel,
            VerificationPurpose purpose
    );

    Optional<VerificationToken> findByIdAndUser_IdAndChannelAndPurpose(
            UUID id,
            UUID userId,
            VerificationChannel channel,
            VerificationPurpose purpose
    );

    List<VerificationToken>
    findAllByUser_IdAndPurposeAndChannelAndUsedAtIsNullAndRevokedAtIsNull(
            UUID userId,
            VerificationPurpose purpose,
            VerificationChannel channel
    );

    long countByUser_IdAndPurposeAndChannelAndCreatedAtAfter(
            UUID userId,
            VerificationPurpose purpose,
            VerificationChannel channel,
            Instant createdAfter
    );
}
