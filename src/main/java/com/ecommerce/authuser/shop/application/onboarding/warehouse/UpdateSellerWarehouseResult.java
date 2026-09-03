package com.ecommerce.authuser.shop.application.onboarding.warehouse;

import com.ecommerce.authuser.shop.domain.OnboardingStep;

import java.util.List;
import java.util.UUID;

public record UpdateSellerWarehouseResult(
        UUID shopId,
        String warehouseName,
        String contactName,
        String contactPhone,
        AddressResult address,
        List<String> carrierPreferences,
        boolean codEnabled,
        OnboardingStep currentStep,
        boolean warehouseCompleted,
        List<String> blockers
) {

    public record AddressResult(
            String line1,
            String line2,
            String ward,
            String district,
            String province,
            String postalCode
    ) {
    }
}
