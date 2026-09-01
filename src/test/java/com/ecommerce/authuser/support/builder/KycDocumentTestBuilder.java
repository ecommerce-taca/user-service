package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.kyc.domain.KycCase;
import com.ecommerce.authuser.kyc.domain.KycDocument;
import com.ecommerce.authuser.support.testdata.KycTestData;

public final class KycDocumentTestBuilder {

    private KycCase kycCase;

    private String documentType =
            KycTestData.DOCUMENT_TYPE;

    private String objectKey =
            KycTestData.OBJECT_KEY;

    private String originalFileName =
            KycTestData.ORIGINAL_FILE_NAME;

    private String contentType =
            KycTestData.PDF_CONTENT_TYPE;

    private int sizeBytes =
            KycTestData.VALID_FILE_SIZE;

    private String sha256 =
            KycTestData.VALID_SHA256;

    private KycDocumentTestBuilder() {
    }

    public static KycDocumentTestBuilder aKycDocument() {
        return new KycDocumentTestBuilder();
    }

    public KycDocumentTestBuilder forKycCase(
            KycCase kycCase
    ) {
        this.kycCase = kycCase;
        return this;
    }

    public KycDocumentTestBuilder withDocumentType(
            String documentType
    ) {
        this.documentType = documentType;
        return this;
    }

    public KycDocumentTestBuilder withObjectKey(
            String objectKey
    ) {
        this.objectKey = objectKey;
        return this;
    }

    public KycDocumentTestBuilder withOriginalFileName(
            String originalFileName
    ) {
        this.originalFileName = originalFileName;
        return this;
    }

    public KycDocumentTestBuilder withContentType(
            String contentType
    ) {
        this.contentType = contentType;
        return this;
    }

    public KycDocumentTestBuilder withSizeBytes(
            int sizeBytes
    ) {
        this.sizeBytes = sizeBytes;
        return this;
    }

    public KycDocumentTestBuilder withSha256(
            String sha256
    ) {
        this.sha256 = sha256;
        return this;
    }

    public KycDocument build() {
        return KycDocument.createUploading(
                kycCase,
                documentType,
                objectKey,
                originalFileName,
                contentType,
                sizeBytes,
                sha256
        );
    }
}