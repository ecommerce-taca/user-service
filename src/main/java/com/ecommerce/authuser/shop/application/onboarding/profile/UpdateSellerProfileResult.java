package com.ecommerce.authuser.shop.application.onboarding.profile;

import com.ecommerce.authuser.shop.domain.OnboardingStep;

import java.util.List;
import java.util.UUID;

public record UpdateSellerProfileResult(
        UUID shopId,
        String name,
        String businessName,
        String taxCode,
        String description,
        String logoObjectKey,
        OnboardingStep currentStep,
        boolean profileCompleted,
        List<String> blockers
) {
}
