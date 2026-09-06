package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.support.testdata.TokenTestData;
import com.ecommerce.authuser.token.domain.PasswordResetToken;
import com.ecommerce.authuser.user.domain.User;

import java.time.Instant;
import java.util.Objects;

public final class PasswordResetTokenTestBuilder {

    private User user;
    private String tokenHash = TokenTestData.PASSWORD_RESET_TOKEN_HASH;
    private Instant expiresAt = TokenTestData.DEFAULT_EXPIRES_AT;

    private PasswordResetTokenTestBuilder() {
    }

    public static PasswordResetTokenTestBuilder aPasswordResetToken() {
        return new PasswordResetTokenTestBuilder();
    }

    public PasswordResetTokenTestBuilder forUser(User user) {
        this.user = user;
        return this;
    }

    public PasswordResetTokenTestBuilder withTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
        return this;
    }

    public PasswordResetTokenTestBuilder expiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public PasswordResetToken build() {
        Objects.requireNonNull(user, "user must not be null");

        return PasswordResetToken.create(
                user,
                tokenHash,
                expiresAt
        );
    }
}
