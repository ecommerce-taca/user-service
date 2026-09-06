package com.ecommerce.authuser.shop.web.register;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import tools.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.Set;

public class RegisterSellerRequest {

    private String name;

    private String businessName;

    private String taxCode;

    private String slug;

    private String description;

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

    @JsonSetter("slug")
    public void setSlug(JsonNode value) {
        slug = readOptionalString(value);
    }

    @JsonSetter("description")
    public void setDescription(JsonNode value) {
        description = readOptionalString(value);
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

    public String slug() {
        return slug;
    }

    public String description() {
        return description;
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
