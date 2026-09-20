package com.ecommerce.authuser.integration.kyc;

import com.ecommerce.authuser.kyc.application.expiry.KycExpiryService;
import com.ecommerce.authuser.kyc.domain.KycCase;
import com.ecommerce.authuser.kyc.domain.KycDocument;
import com.ecommerce.authuser.kyc.repository.KycCaseRepository;
import com.ecommerce.authuser.kyc.repository.KycDocumentRepository;
import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.Shop;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "auth.kyc-expiry.enabled=true")
class KycExpiryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KycExpiryService kycExpiryService;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private KycCaseRepository kycCaseRepository;

    @Autowired
    private KycDocumentRepository kycDocumentRepository;

    @Test
    void expirePendingCases_shouldExpireKycAndPublishOutboxEvent() {
        User seller = UserTestBuilder
                .aUser()
                .withEmail("kyc-expiry-seller@test.com")
                .withEmailNormalized("kyc-expiry-seller@test.com")
                .withFullName("KYC Expiry Seller")
                .build();

        seller = userRepository.saveAndFlush(seller);

        Shop shop = ShopTestBuilder
                .forOwner(seller)
                .withName("KYC Expiry Shop")
                .withSlug("kyc-expiry-shop-" + UUID.randomUUID())
                .withBusinessName("KYC Expiry Shop Business")
                .withDescription("Shop with expired pending KYC")
                .build();

        shop.markKycPending();

        shop = shopRepository.saveAndFlush(shop);

        Instant submittedAt = Instant.now().minusSeconds(7200);

        KycCase kycCase = KycCaseTestBuilder
                .aKycCase()
                .forShop(shop)
                .buildPending(submittedAt);

        ReflectionTestUtils.setField(
                kycCase,
                "expiresAt",
                Instant.now().minusSeconds(3600)
        );

        kycCase = kycCaseRepository.saveAndFlush(kycCase);

        KycDocument document = KycDocumentTestBuilder
                .aKycDocument()
                .forKycCase(kycCase)
                .withDocumentType("BUSINESS_LICENSE")
                .build();

        document.markUploaded(submittedAt.plusSeconds(10));

        document = kycDocumentRepository.saveAndFlush(document);

        int expiredCount = kycExpiryService.expirePendingCases();

        assertThat(expiredCount)
                .isEqualTo(1);

        KycCase persistedCase = kycCaseRepository
                .findById(kycCase.getId())
                .orElseThrow();

        assertThat(persistedCase.getStatus())
                .isEqualTo(KycStatus.EXPIRED);

        assertThat(persistedCase.getDecisionReason())
                .isEqualTo("KYC_EXPIRED");

        Shop persistedShop = shopRepository
                .findById(shop.getId())
                .orElseThrow();

        assertThat(persistedShop.getKycStatus())
                .isEqualTo(KycStatus.EXPIRED);

        List<OutboxEvent> events = outboxEventRepository
                .findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        OutboxAggregateType.SHOP,
                        shop.getId()
                );

        OutboxEvent expiredEvent = events.stream()
                .filter(event -> "shop.kyc.expired"
                        .equals(event.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(expiredEvent.getAggregateType())
                .isEqualTo(OutboxAggregateType.SHOP);

        assertThat(expiredEvent.getAggregateId())
                .isEqualTo(shop.getId());

        assertThat(expiredEvent.getActorUserId())
                .isNull();

        assertThat(expiredEvent.getPartitionKey())
                .isEqualTo(shop.getId().toString());

        Map<String, Object> payload = expiredEvent.getPayloadView();

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

        assertThat(payload.get("expired_documents"))
                .isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<String> expiredDocuments =
                (List<String>) payload.get("expired_documents");

        assertThat(expiredDocuments)
                .containsExactly(document.getDocumentType());
    }
}