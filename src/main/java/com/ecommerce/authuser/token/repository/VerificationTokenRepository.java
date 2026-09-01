package com.ecommerce.authuser.token.repository;

import com.ecommerce.authuser.token.domain.VerificationChannel;
import com.ecommerce.authuser.token.domain.VerificationPurpose;
import com.ecommerce.authuser.token.domain.VerificationToken;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
        select new com.ecommerce.authuser.token.repository.VerificationTokenLookup(
            token.id,
            token.user.id
        )
        from VerificationToken token
        where token.tokenHash = :tokenHash
            and token.channel = :channel
            and token.purpose = :purpose
        """)
    Optional<VerificationTokenLookup> findLookup(
            @Param("tokenHash") String tokenHash,
            @Param("channel") VerificationChannel channel,
            @Param("purpose") VerificationPurpose purpose
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select token
        from VerificationToken token
        where token.id = :tokenId
        """)
    Optional<VerificationToken> findByIdForUpdate(
            @Param("tokenId") UUID tokenId
    );
}
