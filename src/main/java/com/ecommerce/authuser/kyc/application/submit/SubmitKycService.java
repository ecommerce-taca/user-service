package com.ecommerce.authuser.kyc.application.submit;

import com.ecommerce.authuser.kyc.domain.KycCase;
import com.ecommerce.authuser.kyc.domain.KycDocument;
import com.ecommerce.authuser.kyc.domain.KycDocumentStatus;

import com.ecommerce.authuser.kyc.exception.InvalidKycDocumentException;
import com.ecommerce.authuser.kyc.exception.KycAlreadyPendingException;

import com.ecommerce.authuser.kyc.repository.KycCaseRepository;
import com.ecommerce.authuser.kyc.repository.KycDocumentRepository;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;

import com.ecommerce.authuser.rbac.domain.RbacKeys;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.SellerOnboarding;
import com.ecommerce.authuser.shop.domain.Shop;

import com.ecommerce.authuser.shop.exception.SellerEmailNotVerifiedException;
import com.ecommerce.authuser.shop.exception.SellerPermissionDeniedException;
import com.ecommerce.authuser.shop.exception.ShopInvalidStateException;
import com.ecommerce.authuser.shop.exception.ShopNotFoundException;

import com.ecommerce.authuser.shop.repository.SellerOnboardingRepository;
import com.ecommerce.authuser.shop.repository.ShopRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubmitKycService {

    private static final short EVENT_SCHEMA_VERSION = 1;

    private final ShopRepository shopRepository;

    private final SellerOnboardingRepository sellerOnboardingRepository;

    private final UserRoleRepository userRoleRepository;

    private final KycCaseRepository kycCaseRepository;

    private final KycDocumentRepository kycDocumentRepository;

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public SubmitKycResult submit(SubmitKycCommand command) {
        if (command == null || command.userId() == null) {
            throw new InvalidKycDocumentException();
        }

        Shop shop = shopRepository
                .findByOwnerIdForUpdate(command.userId())
                .orElseThrow(ShopNotFoundException::new);

        boolean sellerOwner = userRoleRepository
                .existsByUser_IdAndRole_RoleKeyAndShop_IdAndRevokedAtIsNull(
                        command.userId(),
                        RbacKeys.Roles.SELLER,
                        shop.getId()
                );

        if (!sellerOwner) {
            throw new SellerPermissionDeniedException();
        }

        if (shop.getOwner().getEmailVerifiedAt() == null) {
            throw new SellerEmailNotVerifiedException();
        }

        validateShopState(shop);

        SellerOnboarding onboarding = sellerOnboardingRepository
                .findByShopIdForUpdate(shop.getId())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Seller onboarding not found for shop"
                        )
                );

        if (!onboarding.isProfileCompleted()) {
            throw new InvalidKycDocumentException();
        }

        KycCase kycCase = kycCaseRepository
                .findCurrentByShopIdForUpdate(shop.getId())
                .orElseThrow(InvalidKycDocumentException::new);

        validateKycCaseState(kycCase);

        if (kycDocumentRepository
                .existsByKycCase_IdAndStatusAndDeletedAtIsNull(
                        kycCase.getId(),
                        KycDocumentStatus.UPLOADING
                )) {

            throw new InvalidKycDocumentException();
        }

        List<KycDocumentStatus> validStatuses =
                List.of(
                        KycDocumentStatus.UPLOADED,
                        KycDocumentStatus.VERIFIED
                );

        boolean hasValidDocument = kycDocumentRepository
                .existsByKycCase_IdAndStatusInAndDeletedAtIsNull(
                        kycCase.getId(),
                        validStatuses
                );

        if (!hasValidDocument) {
            throw new InvalidKycDocumentException();
        }

        List<String> documentTypes = kycDocumentRepository
                .findAllByKycCase_IdAndDeletedAtIsNullOrderByCreatedAtAsc(
                        kycCase.getId()
                )
                .stream()
                .filter(
                        document ->
                                document.getStatus() == KycDocumentStatus.UPLOADED
                                        || document.getStatus() == KycDocumentStatus.VERIFIED
                        )
                .map(KycDocument::getDocumentType)
                .distinct()
                .toList();

        Instant now = Instant.now();

        kycCase.submit(now);

        try {
            shop.markKycPending();

        } catch (IllegalStateException ex) {
            throw new ShopInvalidStateException();
        }

        onboarding.completeKycSubmissionStep();

        shopRepository.saveAndFlush(shop);

        kycCaseRepository.saveAndFlush(kycCase);

        sellerOnboardingRepository.saveAndFlush(onboarding);

        createSubmittedEvent(
                shop,
                kycCase,
                documentTypes
        );

        return new SubmitKycResult(
                shop.getId(),
                kycCase.getId(),
                kycCase.getStatus(),
                kycCase.getSubmittedAt()
        );
    }

    private void validateShopState(Shop shop) {

        if (!shop.canSubmitKyc()) {
            throw new ShopInvalidStateException();
        }

        KycStatus status = shop.getKycStatus();

        if (status == KycStatus.PENDING) {
            throw new KycAlreadyPendingException();
        }

        if (status == KycStatus.APPROVED
                || status == KycStatus.SUSPENDED) {

            throw new ShopInvalidStateException();
        }
    }

    private void validateKycCaseState(KycCase kycCase) {
        KycStatus status = kycCase.getStatus();

        if (status == KycStatus.PENDING) {
            throw new KycAlreadyPendingException();
        }

        if (status == KycStatus.APPROVED
                || status == KycStatus.SUSPENDED) {

            throw new ShopInvalidStateException();
        }

        if (status != KycStatus.DRAFT
                && status != KycStatus.NEEDS_INFO
                && status != KycStatus.REJECTED
                && status != KycStatus.EXPIRED) {

            throw new ShopInvalidStateException();
        }
    }

    private void createSubmittedEvent(
            Shop shop,
            KycCase kycCase,
            List<String> documentTypes
    ) {

        OutboxEvent event =
                OutboxEvent.create(
                        OutboxAggregateType.SHOP,
                        shop.getId(),
                        "shop.kyc.submitted",
                        EVENT_SCHEMA_VERSION,
                        shop.getId().toString(),
                        Map.of(
                                "shop_id",
                                shop.getId().toString(),
                                "kyc_case_id",
                                kycCase.getId().toString(),
                                "document_types",
                                documentTypes
                        )
                );

        outboxEventRepository.save(event);
    }
}
