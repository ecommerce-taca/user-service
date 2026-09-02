package com.ecommerce.authuser.mfa.repository;

import com.ecommerce.authuser.mfa.domain.MfaChallenge;
import com.ecommerce.authuser.mfa.domain.MfaPurpose;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MfaChallengeRepository extends JpaRepository<MfaChallenge, UUID> {

    Optional<MfaChallenge> findByIdAndUser_IdAndPurpose(
            UUID challengeId,
            UUID userId,
            MfaPurpose purpose
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select challenge
        from MfaChallenge challenge
        where challenge.id = :challengeId
          and challenge.user.id = :userId
          and challenge.purpose = :purpose
        """)
    Optional<MfaChallenge> findForVerification(
            @Param("challengeId") UUID challengeId,
            @Param("userId") UUID userId,
            @Param("purpose") MfaPurpose purpose
    );

    List<MfaChallenge> findAllByUser_IdAndPurposeAndVerifiedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(
            UUID userId,
            MfaPurpose purpose,
            Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select challenge
        from MfaChallenge challenge
        where challenge.user.id = :userId
            and challenge.purpose = :purpose
            and challenge.verifiedAt is null
            and challenge.revokedAt is null
            and challenge.expiresAt > :now
        order by challenge.createdAt asc
        """)
    List<MfaChallenge> findActiveForUpdate(
            @Param("userId") UUID userId,
            @Param("purpose") MfaPurpose purpose,
            @Param("now") Instant now
    );

    @Query("""
        select new com.ecommerce.authuser.mfa.repository.MfaChallengeLookup(
            challenge.id,
            challenge.user.id,
            challenge.purpose
        )
        from MfaChallenge challenge
        where challenge.id = :challengeId
        """)
    Optional<MfaChallengeLookup> findLookupById(
            @Param("challengeId") UUID challengeId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select challenge
        from MfaChallenge challenge
        where challenge.user.id = :userId
            and challenge.purpose = :purpose
            and challenge.sessionId = :sessionId
            and challenge.verifiedAt is null
            and challenge.revokedAt is null
            and challenge.expiresAt > :now
        order by challenge.createdAt asc
        """)
    List<MfaChallenge> findActiveForSessionForUpdate(
            @Param("userId") UUID userId,
            @Param("purpose") MfaPurpose purpose,
            @Param("sessionId") UUID sessionId,
            @Param("now") Instant now
    );
}
