package com.ecommerce.authuser.shop.web.onboarding.warehouse;

import com.ecommerce.authuser.shop.domain.OnboardingStep;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record UpdateSellerWarehouseResponse(
        Data data,
        Meta meta
) {

    public record Data(
            @JsonProperty("shop_id")
            UUID shopId,

            @JsonProperty("warehouse_name")
            String warehouseName,

            @JsonProperty("contact_name")
            String contactName,

            @JsonProperty("contact_phone")
            String contactPhone,

            Address address,

            @JsonProperty("carrier_preferences")
            List<String> carrierPreferences,

            @JsonProperty("cod_enabled")
            boolean codEnabled,

            @JsonProperty("current_step")
            OnboardingStep currentStep,

            @JsonProperty("warehouse_completed")
            boolean warehouseCompleted,

            List<String> blockers
    ) {
    }

    public record Address(
            String line1,
            String line2,
            String ward,
            String district,
            String province,

            @JsonProperty("postal_code")
            String postalCode
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
