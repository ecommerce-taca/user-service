package com.ecommerce.authuser.integration.kyc;

import com.ecommerce.authuser.kyc.application.submit.SubmitKycCommand;
import com.ecommerce.authuser.kyc.application.submit.SubmitKycResult;
import com.ecommerce.authuser.kyc.application.submit.SubmitKycService;
import com.ecommerce.authuser.kyc.domain.KycCase;
import com.ecommerce.authuser.kyc.domain.KycDocument;
import com.ecommerce.authuser.kyc.domain.KycDocumentStatus;
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
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "auth.kyc-submission.expiry-days=3")
class SubmitKycIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SubmitKycService submitKycService;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private SellerOnboardingRepository sellerOnboardingRepository;

    @Autowired
    private KycCaseRepository kycCaseRepository;

    @Autowired
    private KycDocumentRepository kycDocumentRepository;

    @Test
    void submitKyc_shouldSetExpiresAtAndPublishSubmittedEvent() {
        User seller = UserTestBuilder
                .aUser()
                .withEmail("submit-kyc-seller@test.com")
                .withEmailNormalized("submit-kyc-seller@test.com")
                .withFullName("Submit KYC Seller")
                .build();

        seller.verifyEmail(Instant.now());

        seller = userRepository.saveAndFlush(seller);

        Shop shop = ShopTestBuilder
                .forOwner(seller)
                .withName("Submit KYC Shop")
                .withSlug("submit-kyc-shop-" + UUID.randomUUID())
                .withBusinessName("Submit KYC Shop Business")
                .withDescription("Shop ready to submit KYC")
                .build();

        shop = shopRepository.saveAndFlush(shop);

        SellerOnboarding onboarding = SellerOnboarding.create(shop);

        onboarding.completeProfileStep();

        sellerOnboardingRepository.saveAndFlush(onboarding);

        Role sellerRole = roleRepository
                .findByRoleKey(RbacKeys.Roles.SELLER)
                .orElseThrow();

        UserRole sellerAssignment = UserRole.assign(
                seller,
                sellerRole,
                shop,
                seller.getId()
        );

        userRoleRepository.saveAndFlush(sellerAssignment);

        KycCase kycCase = KycCaseTestBuilder
                .aKycCase()
                .forShop(shop)
                .build();

        kycCase = kycCaseRepository.saveAndFlush(kycCase);

        KycDocument document = KycDocumentTestBuilder
                .aKycDocument()
                .forKycCase(kycCase)
                .withDocumentType("BUSINESS_LICENSE")
                .build();

        document.markUploaded(Instant.now());

        document = kycDocumentRepository.saveAndFlush(document);

        Instant beforeSubmit = Instant.now();

        SubmitKycResult result =
                submitKycService.submit(
                        new SubmitKycCommand(
                                seller.getId()
                        )
                );

        Instant afterSubmit = Instant.now();

        assertThat(result.shopId())
                .isEqualTo(shop.getId());

        assertThat(result.kycCaseId())
                .isEqualTo(kycCase.getId());

        assertThat(result.status())
                .isEqualTo(KycStatus.PENDING);

        KycCase persistedCase = kycCaseRepository
                .findById(kycCase.getId())
                .orElseThrow();

        assertThat(persistedCase.getStatus())
                .isEqualTo(KycStatus.PENDING);

        assertThat(persistedCase.getSubmittedAt())
                .isNotNull();

        assertThat(persistedCase.getExpiresAt())
                .isNotNull();

        assertThat(persistedCase.getExpiresAt())
                .isAfterOrEqualTo(
                        beforeSubmit.plus(Duration.ofDays(3))
                                .minusSeconds(1)
                );

        assertThat(persistedCase.getExpiresAt())
                .isBeforeOrEqualTo(
                        afterSubmit.plus(Duration.ofDays(3))
                                .plusSeconds(1)
                );

        Shop persistedShop = shopRepository
                .findById(shop.getId())
                .orElseThrow();

        assertThat(persistedShop.getKycStatus())
                .isEqualTo(KycStatus.PENDING);

        KycDocument persistedDocument = kycDocumentRepository
                .findById(document.getId())
                .orElseThrow();

        assertThat(persistedDocument.getStatus())
                .isEqualTo(KycDocumentStatus.UPLOADED);

        List<OutboxEvent> events = outboxEventRepository
                .findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        OutboxAggregateType.SHOP,
                        shop.getId()
                );

        OutboxEvent submittedEvent = events.stream()
                .filter(event -> "shop.kyc.submitted"
                        .equals(event.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(submittedEvent.getActorUserId())
                .isEqualTo(seller.getId());

        assertThat(submittedEvent.getPartitionKey())
                .isEqualTo(shop.getId().toString());

        Map<String, Object> payload =
                submittedEvent.getPayloadView();

        assertThat(payload)
                .containsEntry(
                        "shop_id",
                        shop.getId().toString()
                );

        assertThat(payload)
                .containsEntry(
                        "kyc_case_id",
                        kycCase.getId().toString()
                );

        assertThat(payload)
                .containsKey("document_types");

        assertThat(payload.get("document_types"))
                .isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<String> documentTypes =
                (List<String>) payload.get("document_types");

        assertThat(documentTypes)
                .containsExactly("BUSINESS_LICENSE");

        assertThat(payload)
                .containsKey("expires_at");

        assertThat(String.valueOf(payload.get("expires_at")))
                .isEqualTo(
                        persistedCase.getExpiresAt().toString()
                );
    }
}