package com.ecommerce.authuser.kyc.web.presign;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import tools.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.Set;

public class PresignKycDocumentRequest {

    private String documentType;

    private String fileName;

    private String contentType;

    private Long sizeBytes;

    private String sha256;

    private boolean invalidFieldType;

    private final Set<String> unsupportedFields = new LinkedHashSet<>();

    @JsonSetter("document_type")
    public void setDocumentType(JsonNode value) {
        documentType = readRequiredString(value);
    }

    @JsonSetter("file_name")
    public void setFileName(JsonNode value) {
        fileName = readRequiredString(value);
    }

    @JsonSetter("content_type")
    public void setContentType(JsonNode value) {
        contentType = readRequiredString(value);
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

    public String documentType() {
        return documentType;
    }

    public String fileName() {
        return fileName;
    }

    public String contentType() {
        return contentType;
    }

    public Long sizeBytes() {
        return sizeBytes;
    }

    public String sha256() {
        return sha256;
    }

    public boolean invalid() {

        return invalidFieldType
                || !unsupportedFields.isEmpty()
                || documentType == null
                || fileName == null
                || contentType == null
                || sizeBytes == null
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
