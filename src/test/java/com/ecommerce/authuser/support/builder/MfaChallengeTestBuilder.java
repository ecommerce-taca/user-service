package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.mfa.domain.MfaChallenge;
import com.ecommerce.authuser.mfa.domain.MfaPurpose;
import com.ecommerce.authuser.support.testdata.MfaTestData;
import com.ecommerce.authuser.user.domain.User;

import java.time.Instant;

public final class MfaChallengeTestBuilder {

    private User user;

    private MfaPurpose purpose;

    private String codeHash =
            MfaTestData.CODE_HASH;

    private Instant createdAt =
            MfaTestData.CREATED_AT;

    private Instant expiresAt =
            MfaTestData.EXPIRES_AT;

    private MfaChallengeTestBuilder() {
    }

    public static MfaChallengeTestBuilder anMfaChallenge() {
        return new MfaChallengeTestBuilder();
    }

    public MfaChallengeTestBuilder forUser(User user) {
        this.user = user;
        return this;
    }

    public MfaChallengeTestBuilder withPurpose(
            MfaPurpose purpose
    ) {
        this.purpose = purpose;
        return this;
    }

    public MfaChallengeTestBuilder withCodeHash(
            String codeHash
    ) {
        this.codeHash = codeHash;
        return this;
    }

    public MfaChallengeTestBuilder createdAt(
            Instant createdAt
    ) {
        this.createdAt = createdAt;
        return this;
    }

    public MfaChallengeTestBuilder expiresAt(
            Instant expiresAt
    ) {
        this.expiresAt = expiresAt;
        return this;
    }

    public MfaChallenge build() {
        return MfaChallenge.create(
                user,
                purpose,
                codeHash,
                createdAt,
                expiresAt
        );
    }
}
