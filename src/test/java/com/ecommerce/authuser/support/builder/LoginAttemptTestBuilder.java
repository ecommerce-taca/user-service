package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.security.domain.LoginAttempt;
import com.ecommerce.authuser.support.testdata.LoginAttemptTestData;
import com.ecommerce.authuser.user.domain.User;

import java.time.Instant;

public final class LoginAttemptTestBuilder {

    private User user;

    private String identifierHash =
            LoginAttemptTestData.IDENTIFIER_HASH;

    private boolean succeeded = true;

    private String failureReason;

    private String ipHash =
            LoginAttemptTestData.IP_HASH;

    private String userAgentHash =
            LoginAttemptTestData.USER_AGENT_HASH;

    private Instant occurredAt =
            LoginAttemptTestData.OCCURRED_AT;

    private LoginAttemptTestBuilder() {
    }

    public static LoginAttemptTestBuilder aLoginAttempt() {
        return new LoginAttemptTestBuilder();
    }

    public LoginAttemptTestBuilder forUser(User user) {
        this.user = user;
        return this;
    }

    public LoginAttemptTestBuilder withIdentifierHash(
            String identifierHash
    ) {
        this.identifierHash = identifierHash;
        return this;
    }

    public LoginAttemptTestBuilder succeeded() {
        this.succeeded = true;
        this.failureReason = null;
        return this;
    }

    public LoginAttemptTestBuilder failed(String failureReason) {
        this.succeeded = false;
        this.failureReason = failureReason;
        return this;
    }

    public LoginAttemptTestBuilder withIpHash(String ipHash) {
        this.ipHash = ipHash;
        return this;
    }

    public LoginAttemptTestBuilder withUserAgentHash(
            String userAgentHash
    ) {
        this.userAgentHash = userAgentHash;
        return this;
    }

    public LoginAttemptTestBuilder occurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
        return this;
    }

    public LoginAttempt build() {
        if (succeeded) {
            return LoginAttempt.success(
                    user,
                    identifierHash,
                    ipHash,
                    userAgentHash,
                    occurredAt
            );
        }

        return LoginAttempt.failure(
                user,
                identifierHash,
                failureReason,
                ipHash,
                userAgentHash,
                occurredAt
        );
    }
}
