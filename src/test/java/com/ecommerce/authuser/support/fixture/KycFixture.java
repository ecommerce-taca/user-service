package com.ecommerce.authuser.support.fixture;

import com.ecommerce.authuser.kyc.domain.KycCase;
import com.ecommerce.authuser.kyc.repository.KycCaseRepository;
import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.support.builder.KycCaseTestBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class KycFixture {

    private final KycCaseRepository kycCaseRepository;

    public KycFixture(
            KycCaseRepository kycCaseRepository
    ) {
        this.kycCaseRepository = kycCaseRepository;
    }

    public KycCase draft(Shop shop) {
        return kycCaseRepository.save(
                KycCaseTestBuilder
                        .aKycCase()
                        .forShop(shop)
                        .build()
        );
    }

    public KycCase pending(
            Shop shop,
            Instant submittedAt
    ) {
        return kycCaseRepository.save(
                KycCaseTestBuilder
                        .aKycCase()
                        .forShop(shop)
                        .buildPending(submittedAt)
        );
    }

    public KycCase save(KycCase kycCase) {
        return kycCaseRepository.save(kycCase);
    }

    public void deleteAll() {
        kycCaseRepository.deleteAll();
    }
}
