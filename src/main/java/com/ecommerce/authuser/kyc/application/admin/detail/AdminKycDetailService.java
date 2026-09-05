package com.ecommerce.authuser.kyc.application.admin.detail;

import com.ecommerce.authuser.kyc.application.admin.AdminKycAuthorizationService;
import com.ecommerce.authuser.kyc.domain.KycCase;
import com.ecommerce.authuser.kyc.domain.KycDocument;
import com.ecommerce.authuser.kyc.domain.KycDocumentStatus;
import com.ecommerce.authuser.kyc.exception.KycCaseNotFoundException;
import com.ecommerce.authuser.kyc.port.KycObjectStoragePort;
import com.ecommerce.authuser.kyc.repository.KycCaseRepository;
import com.ecommerce.authuser.kyc.repository.KycDocumentRepository;

import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.shop.exception.ShopNotFoundException;
import com.ecommerce.authuser.shop.repository.ShopRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminKycDetailService {

    private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(10);

    private final AdminKycAuthorizationService adminKycAuthorizationService;

    private final ShopRepository shopRepository;

    private final KycCaseRepository kycCaseRepository;

    private final KycDocumentRepository kycDocumentRepository;

    private final KycObjectStoragePort kycObjectStoragePort;

    @Transactional(readOnly = true)
    public AdminKycDetailResult get(AdminKycDetailQuery query) {
        if (query == null) {
            throw new ShopNotFoundException();
        }

        adminKycAuthorizationService.requireKycRead(query.actorUserId());

        if (query.shopId() == null) {
            throw new ShopNotFoundException();
        }

        Shop shop = shopRepository
                .findByIdAndDeletedAtIsNull(query.shopId())
                .orElseThrow(ShopNotFoundException::new);

        KycCase kycCase = kycCaseRepository
                .findFirstByShop_IdOrderBySourceVersionDesc(shop.getId())
                .orElseThrow(KycCaseNotFoundException::new);

        List<KycDocument> documents = kycDocumentRepository
                .findAllByKycCase_IdAndDeletedAtIsNullOrderByCreatedAtAsc(
                        kycCase.getId()
                );

        List<AdminKycDetailResult.DocumentData>
                documentResults =
                documents
                        .stream()
                        .map(this::mapDocument)
                        .toList();

        return new AdminKycDetailResult(
                new AdminKycDetailResult.ShopData(
                        shop.getId(),
                        shop.getName(),
                        shop.getBusinessName(),
                        maskTaxCode(shop.getTaxCode()),
                        shop.getStatus()
                ),

                new AdminKycDetailResult.KycCaseData(
                        kycCase.getId(),
                        kycCase.getStatus(),
                        kycCase.getSubmittedAt(),
                        documentResults
                )
        );
    }

    private AdminKycDetailResult.DocumentData mapDocument(KycDocument document) {
        if (document.getStatus() == KycDocumentStatus.UPLOADING) {
            return new AdminKycDetailResult.DocumentData(
                    document.getId(),
                    document.getDocumentType(),
                    document.getOriginalFileName(),
                    document.getContentType(),
                    document.getSizeBytes(),
                    document.getStatus(),
                    null,
                    null
            );
        }

        KycObjectStoragePort.DownloadResult download =
                kycObjectStoragePort
                        .presignDownload(
                                document.getObjectKey(),
                                DOWNLOAD_TTL
                        );

        return new AdminKycDetailResult.DocumentData(
                document.getId(),
                document.getDocumentType(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getStatus(),
                download.downloadUrl(),
                download.expiresAt()
        );
    }

    private String maskTaxCode(String taxCode) {
        if (taxCode == null || taxCode.isBlank()) {
            return null;
        }

        String normalized = taxCode.strip();

        if (normalized.length() <= 4) {
            return "*".repeat(normalized.length());
        }

        return "*".repeat(
                normalized.length() - 4)
                + normalized.substring(normalized.length() - 4);
    }
}
