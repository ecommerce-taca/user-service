package com.ecommerce.authuser.auth.exception;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MfaStepUpRequiredException extends RuntimeException {

    private final UUID challengeId;

    private final Instant expiresAt;

    private final List<String> methods;

    public MfaStepUpRequiredException(
            UUID challengeId,
            Instant expiresAt,
            List<String> methods
    ) {

        super("Step-up authentication is required");

        this.challengeId = challengeId;

        this.expiresAt = expiresAt;

        this.methods = List.copyOf(methods);
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public List<String> getMethods() {
        return methods;
    }
}
