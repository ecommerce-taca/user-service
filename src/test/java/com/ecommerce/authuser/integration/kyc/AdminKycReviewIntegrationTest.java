package com.ecommerce.authuser.integration.kyc;

import com.ecommerce.authuser.kyc.application.admin.review.AdminKycReviewCommand;
import com.ecommerce.authuser.kyc.application.admin.review.AdminKycReviewResult;
import com.ecommerce.authuser.kyc.application.admin.review.AdminKycReviewService;
import com.ecommerce.authuser.kyc.domain.KycCase;
import com.ecommerce.authuser.kyc.domain.KycDocument;
import com.ecommerce.authuser.kyc.repository.KycCaseRepository;
import com.ecommerce.authuser.kyc.repository.KycDocumentRepository;
import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.rbac.domain.RbacKeys;
import com.ecommerce.authuser.rbac.domain.Role;
import com.ecommerce.authuser.rbac.domain.UserRole;
import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.SellerOnboarding;
import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.shop.domain.ShopStatus;
import com.ecommerce.authuser.shop.repository.SellerOnboardingRepository;
import com.ecommerce.authuser.shop.repository.ShopRepository;
import com.ecommerce.authuser.support.base.BaseIntegrationTest;
import com.ecommerce.authuser.support.builder.KycCaseTestBuilder;
import com.ecommerce.authuser.support.builder.KycDocumentTestBuilder;
import com.ecommerce.authuser.support.builder.ShopTestBuilder;
import com.ecommerce.authuser.support.builder.UserTestBuilder;
import com.ecommerce.authuser.user.domain.User;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminKycReviewIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AdminKycReviewService adminKycReviewService;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private SellerOnboardingRepository sellerOnboardingRepository;

    @Autowired
    private KycCaseRepository kycCaseRepository;

    @Autowired
    private KycDocumentRepository kycDocumentRepository;

    @Test
    void approveKyc_shouldActivateShopAndPublishStatusChangedEvent() {
        User admin = createAdmin();

        User seller = UserTestBuilder
                .aUser()
                .withEmail("kyc-seller@test.com")
                .withEmailNormalized("kyc-seller@test.com")
                .withFullName("KYC Seller")
                .build();

        seller = userRepository.saveAndFlush(seller);

        Shop shop = ShopTestBuilder
                .forOwner(seller)
                .withName("KYC Test Shop")
                .withSlug("kyc-test-shop-" + UUID.randomUUID())
                .withBusinessName("KYC Test Shop Business")
                .withDescription("Shop waiting for KYC approval")
                .build();

        shop.markKycPending();

        shop = shopRepository.saveAndFlush(shop);

        SellerOnboarding onboarding = SellerOnboarding.create(shop);

        sellerOnboardingRepository.saveAndFlush(onboarding);

        Instant submittedAt = Instant.now().minusSeconds(60);

        KycCase kycCase = KycCaseTestBuilder
                .aKycCase()
                .forShop(shop)
                .buildPending(submittedAt);

        kycCase = kycCaseRepository.saveAndFlush(kycCase);

        KycDocument document = KycDocumentTestBuilder
                .aKycDocument()
                .forKycCase(kycCase)
                .build();

        document.markUploaded(submittedAt.plusSeconds(5));

        kycDocumentRepository.saveAndFlush(document);

        UUID sessionId = UUID.randomUUID();

        AdminKycReviewResult result =
                adminKycReviewService.review(
                        new AdminKycReviewCommand(
                                admin.getId(),
                                sessionId,
                                shop.getId(),
                                "APPROVED",
                                null,
                                null,
                                "127.0.0.1"
                        )
                );

        assertThat(result.shopId())
                .isEqualTo(shop.getId());

        assertThat(result.kycCaseId())
                .isEqualTo(kycCase.getId());

        assertThat(result.status())
                .isEqualTo(KycStatus.APPROVED);

        Shop persistedShop = shopRepository
                .findById(shop.getId())
                .orElseThrow();

        assertThat(persistedShop.getKycStatus())
                .isEqualTo(KycStatus.APPROVED);

        assertThat(persistedShop.getStatus())
                .isEqualTo(ShopStatus.ACTIVE);

        List<OutboxEvent> events = outboxEventRepository
                .findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        OutboxAggregateType.SHOP,
                        shop.getId()
                );

        OutboxEvent kycApprovedEvent = events.stream()
                .filter(event -> "shop.kyc.approved"
                        .equals(event.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(kycApprovedEvent.getActorUserId())
                .isEqualTo(admin.getId());

        Map<String, Object> kycApprovedPayload =
                kycApprovedEvent.getPayloadView();

        assertThat(kycApprovedPayload)
                .containsEntry(
                        "shop_id",
                        shop.getId().toString()
                );

        assertThat(kycApprovedPayload)
                .containsEntry(
                        "kyc_case_id",
                        kycCase.getId().toString()
                );

        assertThat(kycApprovedPayload)
                .containsKey("approved_at");

        OutboxEvent statusChangedEvent = events.stream()
                .filter(event -> "shop.status_changed"
                        .equals(event.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(statusChangedEvent.getActorUserId())
                .isEqualTo(admin.getId());

        assertThat(statusChangedEvent.getPartitionKey())
                .isEqualTo(shop.getId().toString());

        Map<String, Object> statusPayload =
                statusChangedEvent.getPayloadView();

        assertThat(statusPayload)
                .containsEntry(
                        "shop_id",
                        shop.getId().toString()
                );

        assertThat(statusPayload)
                .containsEntry(
                        "old_status",
                        ShopStatus.DRAFT.name()
                );

        assertThat(statusPayload)
                .containsEntry(
                        "new_status",
                        ShopStatus.ACTIVE.name()
                );

        assertThat(statusPayload)
                .containsEntry(
                        "reason",
                        "KYC_APPROVED"
                );

        assertThat(statusPayload)
                .containsKey("changed_at");

        assertThat(String.valueOf(statusPayload.get("changed_at")))
                .isNotBlank();
    }

    private User createAdmin() {
        User admin = UserTestBuilder
                .aUser()
                .withEmail("kyc-admin@test.com")
                .withEmailNormalized("kyc-admin@test.com")
                .withFullName("KYC Admin")
                .build();

        admin = userRepository.saveAndFlush(admin);

        Role superAdminRole = roleRepository
                .findByRoleKey(RbacKeys.Roles.SUPER_ADMIN)
                .orElseThrow();

        UserRole assignment = UserRole.assign(
                admin,
                superAdminRole,
                null,
                admin.getId()
        );

        userRoleRepository.saveAndFlush(assignment);

        return admin;
    }
}