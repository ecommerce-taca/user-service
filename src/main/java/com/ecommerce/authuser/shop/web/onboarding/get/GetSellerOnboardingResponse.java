package com.ecommerce.authuser.shop.web.onboarding.get;

import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.OnboardingStep;
import com.ecommerce.authuser.shop.domain.ShopStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record GetSellerOnboardingResponse(
        Data data,
        Meta meta
) {

    public record Data(
            @JsonProperty("shop_id")
            UUID shopId,

            @JsonProperty("current_step")
            OnboardingStep currentStep,

            List<Step> steps,

            @JsonProperty("shop_status")
            ShopStatus shopStatus,

            @JsonProperty("kyc_status")
            KycStatus kycStatus,

            List<String> blockers
    ) {
    }

    public record Step(
            OnboardingStep key,
            boolean completed
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
