package com.ecommerce.authuser.shop.web.onboarding.profile;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import tools.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.Set;

public class UpdateSellerProfileRequest {

    private String name;

    private String businessName;

    private String taxCode;

    private String description;

    private String logoObjectKey;

    private boolean invalidFieldType;

    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    @JsonSetter("name")
    public void setName(JsonNode value) {
        name = readRequiredString(value);
    }

    @JsonSetter("business_name")
    public void setBusinessName(JsonNode value) {
        businessName = readRequiredString(value);
    }

    @JsonSetter("tax_code")
    public void setTaxCode(JsonNode value) {
        taxCode = readRequiredString(value);
    }

    @JsonSetter("description")
    public void setDescription(JsonNode value) {
        description = readOptionalString(value);
    }

    @JsonSetter("logo_object_key")
    public void setLogoObjectKey(JsonNode value) {
        logoObjectKey = readOptionalString(value);
    }

    @JsonAnySetter
    public void captureUnsupportedField(
            String name,
            JsonNode value
    ) {
        unsupportedFields.add(name);
    }

    public String name() {
        return name;
    }

    public String businessName() {
        return businessName;
    }

    public String taxCode() {
        return taxCode;
    }

    public String description() {
        return description;
    }

    public String logoObjectKey() {
        return logoObjectKey;
    }

    public boolean invalid() {
        return invalidFieldType || !unsupportedFields.isEmpty();
    }

    private String readRequiredString(JsonNode value) {

        if (value == null || value.isNull() || !value.isTextual()) {

            invalidFieldType = true;
            return null;
        }

        return value.textValue();
    }

    private String readOptionalString(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }

        if (!value.isTextual()) {
            invalidFieldType = true;
            return null;
        }

        return value.textValue();
    }
}