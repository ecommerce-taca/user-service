package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.support.testdata.TokenTestData;
import com.ecommerce.authuser.token.domain.VerificationChannel;
import com.ecommerce.authuser.token.domain.VerificationPurpose;
import com.ecommerce.authuser.token.domain.VerificationToken;
import com.ecommerce.authuser.user.domain.User;

import java.time.Instant;
import java.util.Objects;

public final class VerificationTokenTestBuilder {

    private User user;
    private VerificationChannel channel =
            TokenTestData.DEFAULT_VERIFICATION_CHANNEL;
    private VerificationPurpose purpose =
            TokenTestData.DEFAULT_VERIFICATION_PURPOSE;
    private String tokenHash =
            TokenTestData.VERIFICATION_TOKEN_HASH;
    private String recipientMasked =
            TokenTestData.DEFAULT_RECIPIENT_MASKED;
    private Instant expiresAt =
            TokenTestData.DEFAULT_EXPIRES_AT;

    private VerificationTokenTestBuilder() {
    }

    public static VerificationTokenTestBuilder aVerificationToken() {
        return new VerificationTokenTestBuilder();
    }

    public VerificationTokenTestBuilder forUser(User user) {
        this.user = user;
        return this;
    }

    public VerificationTokenTestBuilder withChannel(
            VerificationChannel channel
    ) {
        this.channel = channel;
        return this;
    }

    public VerificationTokenTestBuilder withPurpose(
            VerificationPurpose purpose
    ) {
        this.purpose = purpose;
        return this;
    }

    public VerificationTokenTestBuilder withTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
        return this;
    }

    public VerificationTokenTestBuilder withRecipientMasked(
            String recipientMasked
    ) {
        this.recipientMasked = recipientMasked;
        return this;
    }

    public VerificationTokenTestBuilder expiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public VerificationToken build() {
        Objects.requireNonNull(user, "user must not be null");

        return VerificationToken.create(
                user,
                channel,
                purpose,
                tokenHash,
                recipientMasked,
                expiresAt
        );
    }
}
