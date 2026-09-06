package com.ecommerce.authuser.mfa.repository;

import com.ecommerce.authuser.mfa.domain.TwoFactorRecoveryCode;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TwoFactorRecoveryCodeRepository extends JpaRepository<TwoFactorRecoveryCode, UUID> {

    List<TwoFactorRecoveryCode> findAllByCredential_IdAndUsedAtIsNull(
            UUID credentialId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select code
        from TwoFactorRecoveryCode code
        where code.credential.id = :credentialId
          and code.codeHash = :codeHash
          and code.usedAt is null
        """)
    Optional<TwoFactorRecoveryCode> findUsableCodeForUpdate(
            @Param("credentialId") UUID credentialId,
            @Param("codeHash") String codeHash
    );

    @Modifying(flushAutomatically = true)
    @Query("""
        delete from TwoFactorRecoveryCode code
        where code.credential.id = :credentialId
    """)
    int deleteAllByCredentialId(
            @Param("credentialId") UUID credentialId
    );
}
