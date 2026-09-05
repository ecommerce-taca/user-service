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
    
    Optional<KycCase> findFirstByShop_IdOrderBySourceVersionDesc(
            UUID shopId
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<KycCase> findFirstByShop_IdOrderByCreatedAtDesc(
            UUID shopId
    );

    @Query(
            value = """
                select k
                from KycCase k
                join fetch k.shop shop
                where k.status = :status
                    and shop.deletedAt is null
                    and not exists (
                        select newer.id
                        from KycCase newer
                        where newer.shop.id = shop.id
                            and newer.sourceVersion > k.sourceVersion
                    )
                    and (
                        :q is null
                        or locate(
                            lower(:q),
                            lower(shop.name)
                        ) > 0
                        or (
                            :taxCode is not null
                            and shop.taxCode = :taxCode
                        )
                    )
                """,
            countQuery = """
                select count(k)
                from KycCase k
                join k.shop shop
                where k.status = :status
                    and shop.deletedAt is null
                    and not exists (
                        select newer.id
                        from KycCase newer
                        where newer.shop.id = shop.id
                            and newer.sourceVersion > k.sourceVersion
                    )
                    and (
                        :q is null
                        or locate(
                            lower(:q),
                            lower(shop.name)
                        ) > 0
                        or (
                            :taxCode is not null
                            and shop.taxCode = :taxCode
                        )
                    )
                """
    )
    Page<KycCase> findAdminQueue(
            @Param("status") KycStatus status,
            @Param("q") String q,
            @Param("taxCode") String taxCode,
            Pageable pageable
    );
}
