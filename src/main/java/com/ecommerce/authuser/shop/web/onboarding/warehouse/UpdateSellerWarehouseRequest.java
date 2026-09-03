package com.ecommerce.authuser.shop.web.onboarding.warehouse;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class UpdateSellerWarehouseRequest {

    private String warehouseName;
    private String contactName;
    private String contactPhone;

    private WarehouseAddressRequest address;

    private List<String> carrierPreferences;
    
    private Boolean codEnabled;

    private boolean invalidFieldType;

    private final Set<String> unsupportedFields =
            new LinkedHashSet<>();

    @JsonSetter("warehouse_name")
    public void setWarehouseName(JsonNode value) {
        warehouseName = readRequiredString(value);
    }

    @JsonSetter("contact_name")
    public void setContactName(JsonNode value) {
        contactName = readRequiredString(value);
    }

    @JsonSetter("contact_phone")
    public void setContactPhone(JsonNode value) {
        contactPhone = readRequiredString(value);
    }

    @JsonSetter("address")
    public void setAddress(
            WarehouseAddressRequest value
    ) {

        if (value == null) {
            invalidFieldType = true;
            return;
        }

        address = value;
    }

    @JsonSetter("carrier_preferences")
    public void setCarrierPreferences(
            JsonNode value
    ) {

        if (value == null || value.isNull()) {
            carrierPreferences = null;
            return;
        }

        if (!value.isArray()) {
            invalidFieldType = true;
            return;
        }

        List<String> result =
                new ArrayList<>();

        for (JsonNode item : value) {

            if (item == null
                    || item.isNull()
                    || !item.isTextual()) {

                invalidFieldType = true;
                return;
            }

            result.add(
                    item.textValue()
            );
        }

        carrierPreferences =
                List.copyOf(result);
    }

    @JsonSetter("cod_enabled")
    public void setCodEnabled(JsonNode value) {

        if (value == null
                || value.isNull()
                || !value.isBoolean()) {

            invalidFieldType = true;
            return;
        }

        codEnabled = value.booleanValue();
    }

    @JsonAnySetter
    public void captureUnsupportedField(
            String name,
            JsonNode value
    ) {
        unsupportedFields.add(name);
    }

    public String warehouseName() {
        return warehouseName;
    }

    public String contactName() {
        return contactName;
    }

    public String contactPhone() {
        return contactPhone;
    }

    public WarehouseAddressRequest address() {
        return address;
    }

    public List<String> carrierPreferences() {
        return carrierPreferences;
    }

    public Boolean codEnabled() {
        return codEnabled;
    }

    public boolean invalid() {

        return invalidFieldType
                || !unsupportedFields.isEmpty()
                || (address != null
                && address.invalid());
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
}
