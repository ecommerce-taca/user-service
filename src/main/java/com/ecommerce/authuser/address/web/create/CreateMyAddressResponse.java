package com.ecommerce.authuser.address.web.create;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record CreateMyAddressResponse(
        Data data,
        Meta meta
) {

    public record Data(
            UUID id,

            String recipient,

            String phone,

            String line1,

            String line2,

            @JsonProperty("country_code")
            String countryCode,

            @JsonProperty("province_code")
            String provinceCode,

            String province,

            @JsonProperty("ward_code")
            String wardCode,

            String ward,

            String district,

            @JsonProperty("postal_code")
            String postalCode,

            @JsonProperty("is_default")
            boolean defaultAddress,

            @JsonProperty("created_at")
            Instant createdAt,

            @JsonProperty("updated_at")
            Instant updatedAt
    ) {
    }

    public record Meta(
            @JsonProperty("request_id")
            String requestId
    ) {
    }
}
