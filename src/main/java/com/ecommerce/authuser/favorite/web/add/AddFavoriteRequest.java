package com.ecommerce.authuser.favorite.web.add;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import tools.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.Set;

public class AddFavoriteRequest {

    private String productId;

    private boolean invalidFieldType;

    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    @JsonSetter("product_id")
    public void setProductId(JsonNode value) {
        if (value == null || value.isNull() || !value.isTextual()) {
            invalidFieldType = true;
            return;
        }

        productId = value.textValue();
    }

    @JsonAnySetter
    public void captureUnsupportedField(
            String name,
            JsonNode value
    ) {
        unsupportedFields.add(name);
    }

    public String productId() {
        return productId;
    }

    public boolean invalid() {
        return invalidFieldType
                || !unsupportedFields.isEmpty()
                || productId == null;
    }
}
