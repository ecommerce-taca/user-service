package com.ecommerce.authuser.kyc.repository;

import com.ecommerce.authuser.kyc.domain.KycCase;
import com.ecommerce.authuser.shop.domain.KycStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KycCaseRepository extends JpaRepository<KycCase, UUID> {

    Optional<KycCase> findByIdAndShop_Id(
            UUID kycCaseId,
            UUID shopId
    );

    List<KycCase> findAllByShop_IdOrderByCreatedAtDesc(UUID shopId);

    Optional<KycCase> findFirstByShop_IdAndStatusInOrderByUpdatedAtDesc(
            UUID shopId,
            Collection<KycStatus> statuses
    );

    Page<KycCase> findAllByStatus(
            KycStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select k
        from KycCase k
        where k.id = :caseId
        """)
    Optional<KycCase> findByIdForUpdate(@Param("caseId") UUID caseId);
}
