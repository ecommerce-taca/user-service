package com.ecommerce.authuser.support.testdata;

import java.time.Instant;

public final class LoginAttemptTestData {

    public static final String IDENTIFIER_HASH =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    public static final String IP_HASH =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    public static final String USER_AGENT_HASH =
            "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";

    public static final String INVALID_CREDENTIALS =
            "INVALID_CREDENTIALS";

    public static final String ACCOUNT_LOCKED =
            "ACCOUNT_LOCKED";

    public static final Instant OCCURRED_AT =
            Instant.parse("2026-01-01T10:00:00Z");

    private LoginAttemptTestData() {
    }
}

