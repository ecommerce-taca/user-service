package com.ecommerce.authuser.shop.web.register;

import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.OnboardingStep;
import com.ecommerce.authuser.shop.domain.ShopStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record RegisterSellerResponse(
        Data data,
        Meta meta
) {

    public record Data(
            ShopData shop,
            OnboardingData onboarding
    ) {
    }

    public record ShopData(
            UUID id,
            String name,
            String slug,
            ShopStatus status,

            @JsonProperty("kyc_status")
            KycStatus kycStatus
    ) {
    }

    public record OnboardingData(
            @JsonProperty("current_step")
            OnboardingStep currentStep,

            @JsonProperty("completed_steps")
            List<String> completedSteps,

            List<String> blockers
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}