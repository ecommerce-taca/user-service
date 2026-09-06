package com.ecommerce.authuser.user.web.profile;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.Set;

public class UpdateMyProfileRequest {

    private String fullName;

    private boolean phoneProvided;

    private String phone;

    private boolean dateOfBirthProvided;

    private String dateOfBirth;

    private boolean invalidFieldType;

    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    @JsonSetter("full_name")
    public void setFullName(JsonNode value) {

        if (value == null
                || value.isNull()
                || !value.isTextual()) {

            invalidFieldType = true;
            fullName = null;
            return;
        }

        fullName = value.textValue();
    }

    @JsonSetter("phone")
    public void setPhone(JsonNode value) {

        phoneProvided = true;

        if (value == null || value.isNull()) {
            phone = null;
            return;
        }

        if (!value.isTextual()) {
            invalidFieldType = true;
            phone = null;
            return;
        }

        phone = value.textValue();
    }

    @JsonSetter("date_of_birth")
    public void setDateOfBirth(JsonNode value) {

        dateOfBirthProvided = true;

        if (value == null || value.isNull()) {
            dateOfBirth = null;
            return;
        }

        if (!value.isTextual()) {
            invalidFieldType = true;
            dateOfBirth = null;
            return;
        }

        dateOfBirth = value.textValue();
    }

    @JsonAnySetter
    public void captureUnsupportedField(
            String name,
            JsonNode value
    ) {
        unsupportedFields.add(name);
    }

    public String fullName() {
        return fullName;
    }

    public boolean phoneProvided() {
        return phoneProvided;
    }

    public String phone() {
        return phone;
    }

    public boolean dateOfBirthProvided() {
        return dateOfBirthProvided;
    }

    public String dateOfBirth() {
        return dateOfBirth;
    }

    public boolean invalid() {
        return invalidFieldType || !unsupportedFields.isEmpty();
    }
}