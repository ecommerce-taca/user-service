package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.support.testdata.TokenTestData;
import com.ecommerce.authuser.token.domain.RefreshToken;
import com.ecommerce.authuser.user.domain.User;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class RefreshTokenTestBuilder {

    private User user;
    private String tokenHash = TokenTestData.REFRESH_TOKEN_HASH;
    private UUID familyId = TokenTestData.DEFAULT_REFRESH_TOKEN_FAMILY_ID;
    private Instant issuedAt = TokenTestData.DEFAULT_ISSUED_AT;
    private Instant expiresAt = TokenTestData.DEFAULT_EXPIRES_AT;

    private RefreshTokenTestBuilder() {
    }

    public static RefreshTokenTestBuilder aRefreshToken() {
        return new RefreshTokenTestBuilder();
    }

    public RefreshTokenTestBuilder forUser(User user) {
        this.user = user;
        return this;
    }

    public RefreshTokenTestBuilder withTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
        return this;
    }

    public RefreshTokenTestBuilder withFamilyId(UUID familyId) {
        this.familyId = familyId;
        return this;
    }

    public RefreshTokenTestBuilder issuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public RefreshTokenTestBuilder expiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public RefreshToken build() {
        Objects.requireNonNull(user, "user must not be null");

        return RefreshToken.issue(
                user,
                tokenHash,
                familyId,
                issuedAt,
                expiresAt
        );
    }
}
