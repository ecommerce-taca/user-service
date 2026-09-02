package com.ecommerce.authuser.token.repository;

import com.ecommerce.authuser.token.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select token
        from RefreshToken token
        where token.tokenHash = :tokenHash
        """)
    Optional<RefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash")
            String tokenHash
    );

    List<RefreshToken> findAllByFamilyIdAndRevokedAtIsNull(UUID familyId);

    List<RefreshToken> findAllByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(
            UUID userId,
            Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select token
        from RefreshToken token
        where token.familyId = :familyId
        order by token.issuedAt asc
        """)
    List<RefreshToken> findAllByFamilyIdForUpdate(
            @Param("familyId") UUID familyId
    );

    @Query("""
        select new com.ecommerce.authuser.token.repository.RefreshTokenLookup(
            token.id,
            token.user.id,
            token.familyId
        )
        from RefreshToken token
        where token.tokenHash = :tokenHash
        """)
    Optional<RefreshTokenLookup> findLookupByTokenHash(
            @Param("tokenHash") String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select token
        from RefreshToken token
        where token.user.id = :userId
             and token.revokedAt is null
        order by token.issuedAt asc
        """)
    List<RefreshToken> findAllActiveByUserForUpdate(
            @Param("userId") UUID userId
    );
}
