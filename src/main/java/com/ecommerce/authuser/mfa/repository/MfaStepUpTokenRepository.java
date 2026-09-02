package com.ecommerce.authuser.mfa.repository;

import com.ecommerce.authuser.mfa.domain.MfaStepUpToken;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MfaStepUpTokenRepository extends JpaRepository<MfaStepUpToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select token
        from MfaStepUpToken token
        where token.tokenHash = :tokenHash
          and token.user.id = :userId
          and token.sessionId = :sessionId
        """)
    Optional<MfaStepUpToken> findForValidation(
            @Param("tokenHash") String tokenHash,
            @Param("userId") UUID userId,
            @Param("sessionId") UUID sessionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select token
        from MfaStepUpToken token
        where token.user.id = :userId
            and token.revokedAt is null
         order by token.createdAt asc
        """)
    List<MfaStepUpToken> findAllActiveByUserForUpdate(
            @Param("userId")
            UUID userId
    );
}
