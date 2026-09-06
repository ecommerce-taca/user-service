package com.ecommerce.authuser.support.testdata;

import java.time.Instant;
import java.nio.charset.StandardCharsets;

public final class MfaTestData {

    public static final Instant CREATED_AT =
            Instant.parse("2026-01-01T10:00:00Z");

    public static final Instant EXPIRES_AT =
            Instant.parse("2026-01-01T10:05:00Z");

    public static final Instant VERIFIED_AT =
            Instant.parse("2026-01-01T10:01:00Z");

    public static final Instant REVOKED_AT =
            Instant.parse("2026-01-01T10:02:00Z");

    public static final String CODE_HASH =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    public static final String KEY_VERSION =
            "v1";

    public static final byte[] ENCRYPTED_SECRET =
            "encrypted-test-totp-secret"
                    .getBytes(StandardCharsets.UTF_8);

    public static final int MAX_ATTEMPTS = 5;

    private MfaTestData() {
    }
}