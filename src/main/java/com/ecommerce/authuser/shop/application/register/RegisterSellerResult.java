package com.ecommerce.authuser.shop.application.register;

import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.OnboardingStep;
import com.ecommerce.authuser.shop.domain.ShopStatus;

import java.util.List;
import java.util.UUID;

public record RegisterSellerResult(
        ShopResult shop,
        OnboardingResult onboarding
) {

    public record ShopResult(
            UUID id,
            String name,
            String slug,
            ShopStatus status,
            KycStatus kycStatus
    ) {
    }

    public record OnboardingResult(
            OnboardingStep currentStep,
            List<String> completedSteps,
            List<String> blockers
    ) {
    }
}
