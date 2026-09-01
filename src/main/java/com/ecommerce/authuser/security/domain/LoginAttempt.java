package com.ecommerce.authuser.security.domain;

import com.ecommerce.authuser.common.persistence.BooleanToTinyIntConverter;
import com.ecommerce.authuser.user.domain.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;

import org.hibernate.annotations.Immutable;

import java.time.Instant;

@Getter
@Entity
@Immutable
@Table(name = "login_attempts")
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(
            name = "identifier_hash",
            nullable = false,
            length = 64,
            updatable = false
    )
    private String identifierHash;

    @Getter(AccessLevel.NONE)
    @Convert(converter = BooleanToTinyIntConverter.class)
    @Column(name = "succeeded", nullable = false, updatable = false)
    private Boolean succeeded;

    @Column(name = "failure_reason", length = 32, updatable = false)
    private String failureReason;

    @Column(
            name = "ip_hash",
            nullable = false,
            length = 64,
            updatable = false
    )
    private String ipHash;

    @Column(name = "user_agent_hash", length = 64, updatable = false)
    private String userAgentHash;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected LoginAttempt() {
    }

    private static LoginAttempt create(
            User user,
            String identifierHash,
            boolean succeeded,
            String failureReason,
            String ipHash,
            String userAgentHash,
            Instant occurredAt
    ) {
        validateHash(identifierHash, "identifierHash");

        validateHash(ipHash, "ipHash");

        if (userAgentHash != null) {
            validateHash(userAgentHash, "userAgentHash");
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException(
                    "occurredAt must not be null"
            );
        }

        LoginAttempt attempt = new LoginAttempt();

        attempt.user = user;
        attempt.identifierHash = identifierHash.toLowerCase();
        attempt.succeeded = succeeded;
        attempt.failureReason = failureReason;
        attempt.ipHash = ipHash.toLowerCase();
        attempt.userAgentHash = userAgentHash == null ? null : userAgentHash.toLowerCase();
        attempt.occurredAt = occurredAt;

        return attempt;
    }

    private static void validateHash(
            String value,
            String fieldName
    ) {

        if (value == null || !value.matches("^[0-9a-fA-F]{64}$")
        ) {
            throw new IllegalArgumentException(
                    fieldName + " must be a 64-character hex digest"
            );
        }
    }

    public boolean isSucceeded() {
        return Boolean.TRUE.equals(succeeded);
    }

    public static LoginAttempt success(
            User user,
            String identifierHash,
            String ipHash,
            String userAgentHash,
            Instant occurredAt
    ) {

        return create(
                user,
                identifierHash,
                true,
                null,
                ipHash,
                userAgentHash,
                occurredAt
        );
    }

    public static LoginAttempt failure(
            User user,
            String identifierHash,
            String failureReason,
            String ipHash,
            String userAgentHash,
            Instant occurredAt
    ) {

        if (failureReason == null
                || failureReason.isBlank()
                || failureReason.length() > 32
        ) {
            throw new IllegalArgumentException(
                    "Invalid login failure reason"
            );
        }

        return create(
                user,
                identifierHash,
                false,
                failureReason,
                ipHash,
                userAgentHash,
                occurredAt
        );
    }
}
