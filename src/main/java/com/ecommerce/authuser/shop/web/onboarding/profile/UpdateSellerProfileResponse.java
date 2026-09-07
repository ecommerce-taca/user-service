package com.ecommerce.authuser.shop.web.onboarding.profile;

import com.ecommerce.authuser.shop.domain.OnboardingStep;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record UpdateSellerProfileResponse(
        Data data,
        Meta meta
) {

    public record Data(
            @JsonProperty("shop_id")
            UUID shopId,

            String name,

            @JsonProperty("business_name")
            String businessName,

            @JsonProperty("tax_code")
            String taxCode,

            String description,

            @JsonProperty("logo_object_key")
            String logoObjectKey,

            @JsonProperty("current_step")
            OnboardingStep currentStep,

            @JsonProperty("profile_completed")
            boolean profileCompleted,

            List<String> blockers
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}