package com.ecommerce.authuser.kyc.repository;

import com.ecommerce.authuser.kyc.domain.KycDocument;
import com.ecommerce.authuser.kyc.domain.KycDocumentStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {

    Optional<KycDocument> findByIdAndKycCase_Shop_IdAndDeletedAtIsNull(
            UUID documentId,
            UUID shopId
    );

    List<KycDocument> findAllByKycCase_IdAndDeletedAtIsNullOrderByCreatedAtAsc(
            UUID kycCaseId
    );

    long countByKycCase_IdAndDeletedAtIsNull(UUID kycCaseId);

    boolean existsByKycCase_IdAndStatusAndDeletedAtIsNull(
            UUID kycCaseId,
            KycDocumentStatus status
    );

    boolean existsByKycCase_IdAndStatusInAndDeletedAtIsNull(
            UUID kycCaseId,
            Collection<KycDocumentStatus> statuses
    );

    boolean existsByKycCase_IdAndDocumentTypeAndSha256AndDeletedAtIsNull(
            UUID kycCaseId,
            String documentType,
            String sha256
    );
}
