package com.ecommerce.authuser.shop.web.profile;

import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.ShopStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record GetSellerShopResponse(
        Data data,
        Meta meta
) {

    public record Data(
            UUID id,

            String name,

            String slug,

            @JsonProperty("business_name")
            String businessName,

            @JsonProperty("tax_code_masked")
            String taxCodeMasked,

            String description,

            @JsonProperty("logo_url")
            String logoUrl,

            ShopStatus status,

            @JsonProperty("kyc_status")
            KycStatus kycStatus,

            @JsonProperty("warehouse_summary")
            WarehouseSummary warehouseSummary,

            @JsonProperty("bank_summary")
            BankSummary bankSummary,

            @JsonProperty("created_at")
            Instant createdAt,

            @JsonProperty("updated_at")
            Instant updatedAt
    ) {
    }

    public record WarehouseSummary(
            @JsonProperty("warehouse_name")
            String warehouseName,

            String district,

            String province
    ) {
    }

    public record BankSummary(
            @JsonProperty("bank_name")
            String bankName,

            @JsonProperty("masked_account")
            String maskedAccount,

            boolean verified
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
