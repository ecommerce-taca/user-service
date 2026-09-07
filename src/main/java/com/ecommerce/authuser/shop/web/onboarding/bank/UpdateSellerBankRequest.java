package com.ecommerce.authuser.shop.web.onboarding.bank;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import tools.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.Set;

public class UpdateSellerBankRequest {

    private String bankCode;

    private String bankName;

    private String accountName;

    private String accountNumber;

    private Boolean confirmAccountName;

    private boolean invalidFieldType;

    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    @JsonSetter("bank_code")
    public void setBankCode(JsonNode value) {
        bankCode = readRequiredString(value);
    }

    @JsonSetter("bank_name")
    public void setBankName(JsonNode value) {
        bankName = readRequiredString(value);
    }

    @JsonSetter("account_name")
    public void setAccountName(JsonNode value) {
        accountName = readRequiredString(value);
    }

    @JsonSetter("account_number")
    public void setAccountNumber(JsonNode value) {
        accountNumber = readRequiredString(value);
    }

    @JsonSetter("confirm_account_name")
    public void setConfirmAccountName(JsonNode value) {

        if (value == null
                || value.isNull()
                || !value.isBoolean()) {

            invalidFieldType = true;
            return;
        }

        confirmAccountName = value.booleanValue();
    }

    @JsonAnySetter
    public void captureUnsupportedField(
            String name,
            JsonNode value
    ) {
        unsupportedFields.add(
                name
        );
    }

    public String bankCode() {
        return bankCode;
    }

    public String bankName() {
        return bankName;
    }

    public String accountName() {
        return accountName;
    }

    public String accountNumber() {
        return accountNumber;
    }

    public Boolean confirmAccountName() {
        return confirmAccountName;
    }

    public boolean invalid() {

        return invalidFieldType
                || !unsupportedFields.isEmpty()
                || bankCode == null
                || bankName == null
                || accountName == null
                || accountNumber == null
                || confirmAccountName == null;
    }

    private String readRequiredString(
            JsonNode value
    ) {

        if (value == null
                || value.isNull()
                || !value.isTextual()) {

            invalidFieldType = true;
            return null;
        }

        return value.textValue();
    }

    @Override
    public String toString() {

        return "UpdateSellerBankRequest{"
                + "bankCode='"
                + bankCode
                + '\''
                + ", bankName='"
                + bankName
                + '\''
                + ", accountName='[REDACTED]'"
                + ", accountNumber='[REDACTED]'"
                + ", confirmAccountName="
                + confirmAccountName
                + ", invalidFieldType="
                + invalidFieldType
                + ", unsupportedFields="
                + unsupportedFields
                + '}';
    }
}
