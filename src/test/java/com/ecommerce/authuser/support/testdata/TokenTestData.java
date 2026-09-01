package com.ecommerce.authuser.support.testdata;

import com.ecommerce.authuser.token.domain.VerificationChannel;
import com.ecommerce.authuser.token.domain.VerificationPurpose;

import java.time.Instant;
import java.util.UUID;

public final class TokenTestData {

    public static final String PASSWORD_RESET_TOKEN_HASH =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    public static final String REFRESH_TOKEN_HASH =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    public static final String VERIFICATION_TOKEN_HASH =
            "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";

    public static final UUID DEFAULT_REFRESH_TOKEN_FAMILY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    public static final Instant DEFAULT_ISSUED_AT =
            Instant.parse("2029-01-01T00:00:00Z");

    public static final Instant DEFAULT_EXPIRES_AT =
            Instant.parse("2030-01-01T00:00:00Z");

    public static final String DEFAULT_RECIPIENT_MASKED =
            "u***@example.com";

    public static final VerificationChannel DEFAULT_VERIFICATION_CHANNEL =
            VerificationChannel.EMAIL;

    public static final VerificationPurpose DEFAULT_VERIFICATION_PURPOSE =
            VerificationPurpose.EMAIL_VERIFY;

    private TokenTestData() {
    }
}
