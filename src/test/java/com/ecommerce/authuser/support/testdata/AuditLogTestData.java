package com.ecommerce.authuser.support.testdata;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class AuditLogTestData {

    public static final String USER_CREATED =
            "USER_CREATED";

    public static final String USER_SUSPENDED =
            "USER_SUSPENDED";

    public static final String KYC_APPROVED =
            "KYC_APPROVED";

    public static final String KYC_REJECTED =
            "KYC_REJECTED";

    public static final String AUTH_SIGNOUT =
            "AUTH_SIGNOUT";

    public static final String IP_HASH =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    public static final Instant OCCURRED_AT =
            Instant.parse("2026-01-01T10:00:00Z");

    public static final String REVIEW_REASON =
            "KYC information has been reviewed.";

    public static Map<String, Object> emptyMetadata() {
        return new HashMap<>();
    }

    public static Map<String, Object> metadata(
            String key,
            Object value
    ) {
        return new HashMap<>(
                Map.of(
                        key,
                        value
                )
        );
    }

    private AuditLogTestData() {
    }
}
