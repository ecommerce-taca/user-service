package com.ecommerce.authuser.support.testdata;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OutboxTestData {

    public static final String USER_CREATED =
            "USER_CREATED";

    public static final String SHOP_CREATED =
            "SHOP_CREATED";

    public static final String KYC_SUBMITTED =
            "KYC_SUBMITTED";

    public static final short SCHEMA_VERSION =
            1;

    public static final String ERROR_PUBLISH_FAILED =
            "PUBLISH_FAILED";

    public static Map<String, Object> userCreatedPayload(
            UUID userId
    ) {
        return new HashMap<>(
                Map.of(
                        "event",
                        USER_CREATED,
                        "userId",
                        userId.toString()
                )
        );
    }

    public static Map<String, Object> kycSubmittedPayload(
            UUID kycCaseId
    ) {
        return new HashMap<>(
                Map.of(
                        "event",
                        KYC_SUBMITTED,
                        "kycCaseId",
                        kycCaseId.toString()
                )
        );
    }

    private OutboxTestData() {
    }
}
