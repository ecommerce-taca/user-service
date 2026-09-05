package com.ecommerce.authuser.kyc.application.admin.review;

import com.ecommerce.authuser.audit.domain.AuditLog;
import com.ecommerce.authuser.audit.domain.AuditTargetType;
import com.ecommerce.authuser.audit.repository.AuditLogRepository;

import com.ecommerce.authuser.auth.exception.MfaStepUpRequiredException;

import com.ecommerce.authuser.kyc.application.admin.AdminKycAuthorizationService;

import com.ecommerce.authuser.kyc.domain.KycCase;
import com.ecommerce.authuser.kyc.domain.KycDocument;
import com.ecommerce.authuser.kyc.domain.KycDocumentStatus;

import com.ecommerce.authuser.kyc.exception.AdminKycPermissionDeniedException;
import com.ecommerce.authuser.kyc.exception.InvalidKycReviewRequestException;
import com.ecommerce.authuser.kyc.exception.KycCaseNotFoundException;
import com.ecommerce.authuser.kyc.exception.KycDecisionConflictException;

import com.ecommerce.authuser.kyc.repository.KycCaseRepository;
import com.ecommerce.authuser.kyc.repository.KycDocumentRepository;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;

import com.ecommerce.authuser.security.service.AuditValueHasher;

import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.SellerOnboarding;
import com.ecommerce.authuser.shop.domain.Shop;

import com.ecommerce.authuser.shop.exception.ShopInvalidStateException;
import com.ecommerce.authuser.shop.exception.ShopNotFoundException;

import com.ecommerce.authuser.shop.repository.SellerOnboardingRepository;
import com.ecommerce.authuser.shop.repository.ShopRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminKycReviewService {

    private static final short EVENT_SCHEMA_VERSION = 1;

    private final AdminKycAuthorizationService authorizationService;

    private final AdminKycStepUpService stepUpService;

    private final ShopRepository shopRepository;

    private final SellerOnboardingRepository sellerOnboardingRepository;

    private final KycCaseRepository kycCaseRepository;

    private final KycDocumentRepository kycDocumentRepository;

    private final AuditLogRepository auditLogRepository;

    private final OutboxEventRepository outboxEventRepository;

    private final AuditValueHasher auditValueHasher;

    @Transactional(
            noRollbackFor = MfaStepUpRequiredException.class
    )
    public AdminKycReviewResult review(
            AdminKycReviewCommand command
    ) {

        if (command == null
                || command.actorUserId() == null) {
            throw new AdminKycPermissionDeniedException();
        }

        authorizationService.requireKycDecide(
                command.actorUserId()
        );

        if (command.shopId() == null) {
            throw new ShopNotFoundException();
        }

        AdminKycDecision decision = parseDecision(command.decision());

        Instant now = Instant.now();

        if (decision == AdminKycDecision.REJECTED) {

            stepUpService.require(
                    command.actorUserId(),
                    command.sessionId(),
                    command.stepUpToken(),
                    now
            );
        }

        Shop shop = shopRepository
                .findByIdForUpdate(command.shopId())
                .orElseThrow(ShopNotFoundException::new);

        SellerOnboarding onboarding = sellerOnboardingRepository
                .findByShopIdForUpdate(shop.getId())
                .orElseThrow(ShopInvalidStateException::new);

        KycCase kycCase =kycCaseRepository
                .findCurrentByShopIdForUpdate(shop.getId())
                .orElseThrow(KycCaseNotFoundException::new);

        if (kycCase.getStatus() != KycStatus.PENDING) {
            throw new KycDecisionConflictException();
        }

        if (shop.getKycStatus() != KycStatus.PENDING) {
            throw new ShopInvalidStateException();
        }

        KycStatus targetStatus = toKycStatus(decision);

        if (decision == AdminKycDecision.APPROVED) {
            verifyDocumentsForApproval(
                    kycCase,
                    now
            );
        }

        KycStatus oldCaseStatus = kycCase.getStatus();

        KycStatus oldShopKycStatus = shop.getKycStatus();

        try {
            kycCase.review(
                    targetStatus,
                    command.actorUserId(),
                    command.reason(),
                    now
            );
        } catch (IllegalArgumentException ex) {
            throw new InvalidKycReviewRequestException();
        } catch (IllegalStateException ex) {
            throw new KycDecisionConflictException();
        }

        try {
            shop.applyKycDecision(targetStatus);
        } catch (IllegalArgumentException ex) {
            throw new InvalidKycReviewRequestException();
        } catch (IllegalStateException ex) {
            throw new ShopInvalidStateException();
        }

        boolean reopenKyc =
                decision == AdminKycDecision.NEEDS_INFO
                        || decision
                        == AdminKycDecision.REJECTED;

        if (reopenKyc) {
            onboarding.reopenKycStep();
        }

        shopRepository.saveAndFlush(shop);

        kycCaseRepository.saveAndFlush(kycCase);

        if (reopenKyc) {
            sellerOnboardingRepository
                    .saveAndFlush(onboarding);
        }

        createAudit(
                command,
                shop,
                kycCase,
                oldCaseStatus,
                oldShopKycStatus,
                now
        );

        createOutboxEvent(
                command,
                shop,
                kycCase
        );

        return new AdminKycReviewResult(
                shop.getId(),
                kycCase.getId(),
                kycCase.getStatus(),
                kycCase.getReviewedAt()
        );
    }

    private void verifyDocumentsForApproval(
            KycCase kycCase,
            Instant now
    ) {

        List<KycDocument> documents =
                kycDocumentRepository
                        .findAllLiveByKycCaseIdForUpdate(
                                kycCase.getId()
                        );

        if (documents.isEmpty()) {
            throw new KycDecisionConflictException();
        }

        boolean valid =
                documents.stream()
                        .allMatch(
                                document ->
                                        document.getStatus()
                                                == KycDocumentStatus.UPLOADED

                                                || document.getStatus()
                                                == KycDocumentStatus.VERIFIED
                        );

        if (!valid) {
            throw new KycDecisionConflictException();
        }

        documents.stream()
                .filter(
                        document ->
                                document.getStatus()
                                        == KycDocumentStatus.UPLOADED
                )
                .forEach(
                        document ->
                                document.markVerified(
                                        now
                                )
                );

        kycDocumentRepository.saveAll(documents);
    }

    private AdminKycDecision parseDecision(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidKycReviewRequestException();
        }

        try {

            return AdminKycDecision.valueOf(
                    value.strip()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (IllegalArgumentException ex) {
            throw new InvalidKycReviewRequestException();
        }
    }

    private KycStatus toKycStatus(AdminKycDecision decision) {

        return switch (decision) {

            case APPROVED ->
                    KycStatus.APPROVED;

            case NEEDS_INFO ->
                    KycStatus.NEEDS_INFO;

            case REJECTED ->
                    KycStatus.REJECTED;
        };
    }

    private void createAudit(
            AdminKycReviewCommand command,
            Shop shop,
            KycCase kycCase,
            KycStatus oldCaseStatus,
            KycStatus oldShopKycStatus,
            Instant now
    ) {

        String clientIp =
                command.clientIp() == null
                        || command.clientIp().isBlank()
                        ? "unknown"
                        : command.clientIp().strip();

        String ipHash = auditValueHasher.hash(clientIp);

        AuditLog audit =
                AuditLog.create(
                        command.actorUserId(),
                        "KYC_REVIEW",
                        AuditTargetType.KYC,
                        kycCase.getId(),
                        kycCase.getDecisionReason(),
                        Map.of(
                                "shop_id",
                                shop.getId().toString(),

                                "source_version",
                                kycCase.getSourceVersion(),

                                "old_case_status",
                                oldCaseStatus.name(),

                                "new_case_status",
                                kycCase.getStatus().name(),

                                "old_shop_kyc_status",
                                oldShopKycStatus.name(),

                                "new_shop_kyc_status",
                                shop.getKycStatus().name()
                        ),
                        ipHash,
                        now
                );

        auditLogRepository.save(audit);
    }

    private void createOutboxEvent(
            AdminKycReviewCommand command,
            Shop shop,
            KycCase kycCase
    ) {

        String eventType =
                switch (kycCase.getStatus()) {

                    case APPROVED ->
                            "shop.kyc.approved";

                    case NEEDS_INFO ->
                            "shop.kyc.needs_info";

                    case REJECTED ->
                            "shop.kyc.rejected";

                    default ->
                            throw new IllegalStateException(
                                    "Unsupported reviewed KYC state"
                            );
                };

        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("shop_id", shop.getId().toString());

        payload.put(
                "kyc_case_id",
                kycCase.getId().toString()
        );

        if (kycCase.getStatus() == KycStatus.APPROVED) {
            payload.put(
                    "approved_at",
                    kycCase.getReviewedAt()
                            .toString()
            );

        } else {
            payload.put(
                    "reason",
                    kycCase.getDecisionReason()
            );
        }

        OutboxEvent event =
                OutboxEvent.create(
                        OutboxAggregateType.SHOP,
                        shop.getId(),
                        eventType,
                        EVENT_SCHEMA_VERSION,
                        shop.getId().toString(),
                        payload
                );

        outboxEventRepository.save(event);
    }
}
