package com.ecommerce.authuser.kyc.web.complete;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import tools.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.Set;

public class CompleteKycDocumentRequest {

    private String documentId;

    private String objectKey;

    private Long sizeBytes;

    private String contentType;

    private String sha256;

    private boolean invalidFieldType;

    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    @JsonSetter("document_id")
    public void setDocumentId(JsonNode value) {
        documentId = readRequiredString(value);
    }

    @JsonSetter("object_key")
    public void setObjectKey(JsonNode value) {
        objectKey = readRequiredString(value);
    }

    @JsonSetter("size_bytes")
    public void setSizeBytes(JsonNode value) {

        if (value == null
                || value.isNull()
                || !value.isIntegralNumber()
                || !value.canConvertToLong()) {

            invalidFieldType = true;
            return;
        }

        sizeBytes = value.longValue();
    }

    @JsonSetter("content_type")
    public void setContentType(JsonNode value) {
        contentType = readRequiredString(value);
    }

    @JsonSetter("sha256")
    public void setSha256(JsonNode value) {
        sha256 = readRequiredString(value);
    }

    @JsonAnySetter
    public void captureUnsupportedField(
            String name,
            JsonNode value
    ) {
        unsupportedFields.add(name);
    }

    public String documentId() {
        return documentId;
    }

    public String objectKey() {
        return objectKey;
    }

    public Long sizeBytes() {
        return sizeBytes;
    }

    public String contentType() {
        return contentType;
    }

    public String sha256() {
        return sha256;
    }

    public boolean invalid() {

        return invalidFieldType
                || !unsupportedFields.isEmpty()
                || documentId == null
                || objectKey == null
                || sizeBytes == null
                || contentType == null
                || sha256 == null;
    }

    private String readRequiredString(JsonNode value) {
        if (value == null || value.isNull() || !value.isTextual()) {

            invalidFieldType = true;
            return null;
        }

        return value.textValue();
    }
}