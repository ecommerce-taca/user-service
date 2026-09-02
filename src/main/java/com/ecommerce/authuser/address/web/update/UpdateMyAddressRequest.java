package com.ecommerce.authuser.address.web.update;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import tools.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.Set;

public class UpdateMyAddressRequest {

    private String recipient;

    private String phone;

    private String line1;

    private String line2;

    private String ward;

    private String district;

    private String province;

    private String postalCode;

    private boolean defaultProvided;

    private boolean defaultRequested;

    private boolean invalidFieldType;

    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    @JsonSetter("recipient")
    public void setRecipient(JsonNode value) {
        recipient = readRequiredString(value);
    }

    @JsonSetter("phone")
    public void setPhone(JsonNode value) {
        phone = readRequiredString(value);
    }

    @JsonSetter("line1")
    public void setLine1(JsonNode value) {
        line1 = readRequiredString(value);
    }

    @JsonSetter("line2")
    public void setLine2(JsonNode value) {
        line2 = readOptionalString(value);
    }

    @JsonSetter("ward")
    public void setWard(JsonNode value) {
        ward = readRequiredString(value);
    }

    @JsonSetter("district")
    public void setDistrict(JsonNode value) {
        district = readRequiredString(value);
    }

    @JsonSetter("province")
    public void setProvince(JsonNode value) {
        province = readRequiredString(value);
    }

    @JsonSetter("postal_code")
    public void setPostalCode(JsonNode value) {
        postalCode = readOptionalString(value);
    }

    @JsonSetter("is_default")
    public void setDefaultRequested(JsonNode value) {

        defaultProvided = true;

        if (value == null
                || value.isNull()
                || !value.isBoolean()) {

            invalidFieldType = true;
            return;
        }

        defaultRequested =
                value.booleanValue();
    }

    @JsonAnySetter
    public void captureUnsupportedField(
            String name,
            JsonNode value
    ) {

        unsupportedFields.add(name);
    }

    public String recipient() {
        return recipient;
    }

    public String phone() {
        return phone;
    }

    public String line1() {
        return line1;
    }

    public String line2() {
        return line2;
    }

    public String ward() {
        return ward;
    }

    public String district() {
        return district;
    }

    public String province() {
        return province;
    }

    public String postalCode() {
        return postalCode;
    }

    public boolean defaultProvided() {
        return defaultProvided;
    }

    public boolean defaultRequested() {
        return defaultRequested;
    }

    public boolean invalid() {
        return invalidFieldType || !unsupportedFields.isEmpty();
    }

    private String readRequiredString(JsonNode value) {
        if (value == null
                || value.isNull()
                || !value.isTextual()) {

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