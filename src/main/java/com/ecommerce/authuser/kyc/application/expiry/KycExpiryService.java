package com.ecommerce.authuser.kyc.application.expiry;

import com.ecommerce.authuser.kyc.domain.KycCase;
import com.ecommerce.authuser.kyc.domain.KycDocument;
import com.ecommerce.authuser.kyc.repository.KycCaseRepository;
import com.ecommerce.authuser.kyc.repository.KycDocumentRepository;
import com.ecommerce.authuser.kyc.infrastructure.scheduler.KycExpiryProperties;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;

import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.shop.repository.ShopRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KycExpiryService {

    private static final short EVENT_SCHEMA_VERSION = 1;

    private final KycCaseRepository kycCaseRepository;

    private final KycDocumentRepository kycDocumentRepository;

    private final ShopRepository shopRepository;

    private final OutboxEventRepository outboxEventRepository;

    private final KycExpiryProperties properties;

    @Transactional
    public int expirePendingCases() {
        if (!properties.isEnabled()) {
            return 0;
        }

        Instant now = Instant.now();

        List<KycCase> expiredCases =
                kycCaseRepository.findExpiredPendingForUpdate(
                        now,
                        PageRequest.of(0, properties.getBatchSize())
                );

        for (KycCase kycCase : expiredCases) {
            expireOne(kycCase, now);
        }

        return expiredCases.size();
    }

    private void expireOne(
            KycCase kycCase,
            Instant now
    ) {
        Shop shop = kycCase.getShop();

        kycCase.expire(now);

        shop.expireKyc();

        kycCaseRepository.save(kycCase);

        shopRepository.save(shop);

        createExpiredEvent(
                shop,
                kycCase
        );
    }

    private void createExpiredEvent(
            Shop shop,
            KycCase kycCase
    ) {
        List<String> expiredDocuments =
                kycDocumentRepository
                        .findAllByKycCase_IdAndDeletedAtIsNull(
                                kycCase.getId()
                        )
                        .stream()
                        .map(KycDocument::getDocumentType)
                        .distinct()
                        .toList();

        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("shop_id", shop.getId().toString());

        payload.put(
                "kyc_case_id",
                kycCase.getId().toString()
        );

        payload.put(
                "expired_documents",
                expiredDocuments
        );

        OutboxEvent event =
                OutboxEvent.createWithActor(
                        OutboxAggregateType.SHOP,
                        shop.getId(),
                        null,
                        "shop.kyc.expired",
                        EVENT_SCHEMA_VERSION,
                        shop.getId().toString(),
                        payload
                );

        outboxEventRepository.save(event);
    }
}