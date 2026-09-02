package com.ecommerce.authuser.token.repository;

import com.ecommerce.authuser.token.domain.PasswordResetToken;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select token
        from PasswordResetToken token
        where token.tokenHash = :tokenHash
        """)
    Optional<PasswordResetToken> findByTokenHashForUpdate(
            @Param("tokenHash")
            String tokenHash
    );

    List<PasswordResetToken>
    findAllByUser_IdAndUsedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(
            UUID userId,
            Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select token
        from PasswordResetToken token
        where token.user.id = :userId
            and token.usedAt is null
            and token.revokedAt is null
            and token.expiresAt > :now
        order by token.createdAt asc
        """)
    List<PasswordResetToken> findActiveForUpdate(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    @Query("""
        select new com.ecommerce.authuser.token.repository.PasswordResetTokenLookup(
            token.id,
            token.user.id
        )
        from PasswordResetToken token
        where token.tokenHash = :tokenHash
        """)
    Optional<PasswordResetTokenLookup> findLookupByTokenHash(
            @Param("tokenHash") String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select token
        from PasswordResetToken token
        where token.id = :tokenId
        """)
    Optional<PasswordResetToken> findByIdForUpdate(
            @Param("tokenId") UUID tokenId
    );
}
