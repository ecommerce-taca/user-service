package com.ecommerce.authuser.shop.web.profile;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import tools.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.Set;

public class UpdateSellerShopRequest {

    private boolean nameProvided;
    private String name;

    private boolean descriptionProvided;
    private String description;

    private boolean logoObjectKeyProvided;
    private String logoObjectKey;

    private boolean invalidFieldType;

    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    @JsonSetter("name")
    public void setName(JsonNode value) {

        nameProvided = true;

        if (value == null
                || value.isNull()
                || !value.isTextual()) {

            invalidFieldType = true;
            name = null;
            return;
        }

        name = value.textValue();
    }

    @JsonSetter("description")
    public void setDescription(JsonNode value) {
        descriptionProvided = true;

        if (value == null || value.isNull()) {
            description = null;
            return;
        }

        if (!value.isTextual()) {
            invalidFieldType = true;
            description = null;
            return;
        }

        description = value.textValue();
    }

    @JsonSetter("logo_object_key")
    public void setLogoObjectKey(JsonNode value) {

        logoObjectKeyProvided = true;

        if (value == null || value.isNull()) {
            logoObjectKey = null;
            return;
        }

        if (!value.isTextual()) {
            invalidFieldType = true;
            logoObjectKey = null;
            return;
        }

        logoObjectKey = value.textValue();
    }

    @JsonAnySetter
    public void captureUnsupportedField(
            String name,
            JsonNode value
    ) {
        unsupportedFields.add(name);
    }

    public boolean nameProvided() {
        return nameProvided;
    }

    public String name() {
        return name;
    }

    public boolean descriptionProvided() {
        return descriptionProvided;
    }

    public String description() {
        return description;
    }

    public boolean logoObjectKeyProvided() {
        return logoObjectKeyProvided;
    }

    public String logoObjectKey() {
        return logoObjectKey;
    }

    public boolean invalid() {

        return invalidFieldType
                || !unsupportedFields.isEmpty()
                || (
                !nameProvided
                        && !descriptionProvided
                        && !logoObjectKeyProvided
        );
    }
}
