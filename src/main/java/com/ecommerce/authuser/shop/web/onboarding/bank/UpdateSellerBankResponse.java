package com.ecommerce.authuser.shop.web.onboarding.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateSellerBankResponse(
        Data data,
        Meta meta
) {

    public record Data(
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
