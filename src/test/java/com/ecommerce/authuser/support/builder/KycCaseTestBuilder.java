package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.kyc.domain.KycCase;
import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.Shop;

import java.time.Instant;

public class KycCaseTestBuilder {

    private Shop shop;

    private int sourceVersion = 1;

    private KycCaseTestBuilder() {
    }

    public static KycCaseTestBuilder aKycCase() {
        return new KycCaseTestBuilder();
    }

    public KycCaseTestBuilder forShop(Shop shop) {
        this.shop = shop;
        return this;
    }

    public KycCaseTestBuilder sourceVersion(int sourceVersion) {
        this.sourceVersion = sourceVersion;
        return this;
    }

    public KycCase build() {
        if (shop == null) {
            throw new IllegalStateException(
                    "shop must be provided"
            );
        }

        return KycCase.createDraft(
                shop,
                sourceVersion
        );
    }

    public KycCase buildSubmitted(
            Instant submittedAt
    ) {
        KycCase kycCase = build();

        kycCase.submit(submittedAt);

        return kycCase;
    }

    public KycCase buildPending(
            Instant submittedAt
    ) {
        return buildSubmitted(submittedAt);
    }
}