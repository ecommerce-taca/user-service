package com.ecommerce.authuser.mfa.repository;

import com.ecommerce.authuser.mfa.domain.TwoFactorCredential;
import com.ecommerce.authuser.mfa.domain.TwoFactorStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TwoFactorCredentialRepository extends JpaRepository<TwoFactorCredential, UUID> {

    Optional<TwoFactorCredential> findByUser_Id(UUID userId);

    boolean existsByUser_IdAndStatus(
            UUID userId,
            TwoFactorStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select credential
        from TwoFactorCredential credential
        where credential.user.id = :userId
        """)
    Optional<TwoFactorCredential> findByUserIdForUpdate(
            @Param("userId") UUID userId
    );
}
