package com.ecommerce.authuser.support.testdata;

import java.time.Instant;

public final class KycTestData {

    public static final Instant CREATED_AT =
            Instant.parse("2026-01-01T10:00:00Z");

    public static final Instant SUBMITTED_AT =
            Instant.parse("2026-01-01T10:01:00Z");

    public static final Instant REVIEWED_AT =
            Instant.parse("2026-01-01T11:00:00Z");

    public static final Instant DOCUMENT_UPLOADED_AT =
            Instant.parse("2026-01-01T10:02:00Z");

    public static final Instant DOCUMENT_VERIFIED_AT =
            Instant.parse("2026-01-01T10:03:00Z");

    public static final String DOCUMENT_TYPE =
            "IDENTITY_CARD";

    public static final String OBJECT_KEY =
            "kyc/test/document-001.pdf";

    public static final String ORIGINAL_FILE_NAME =
            "identity-card.pdf";

    public static final String PDF_CONTENT_TYPE =
            "application/pdf";

    public static final String JPEG_CONTENT_TYPE =
            "image/jpeg";

    public static final String PNG_CONTENT_TYPE =
            "image/png";

    public static final int VALID_FILE_SIZE =
            1024;

    public static final String VALID_SHA256 =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    public static final String VALID_REVIEW_REASON =
            "Document information requires additional verification.";

    private KycTestData() {
    }
}
