package com.ecommerce.authuser.mfa.domain;

import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.user.domain.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;

import java.time.Instant;

import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "mfa_step_up_tokens",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_mfa_step_up_token_hash",
                        columnNames = "token_hash"
                ),
                @UniqueConstraint(
                        name = "uq_mfa_step_up_challenge",
                        columnNames = "challenge_id"
                )
        }
)
public class MfaStepUpToken {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(
            name = "session_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID sessionId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false, updatable = false)
    private MfaChallenge challenge;

    @Getter(AccessLevel.NONE)
    @Column(
            name = "token_hash",
            nullable = false,
            updatable = false,
            length = 64,
            columnDefinition = "CHAR(64)"
    )
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MfaStepUpToken() {
    }

    public static MfaStepUpToken issue(
            User user,
            UUID sessionId,
            MfaChallenge challenge,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt
    ) {

        Objects.requireNonNull(user);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(challenge);
        Objects.requireNonNull(issuedAt);
        Objects.requireNonNull(expiresAt);

        if (challenge.getPurpose() != MfaPurpose.STEP_UP) {
            throw new IllegalArgumentException(
                    "Challenge must be STEP_UP"
            );
        }

        if (!challenge.belongsToSession(sessionId)) {
            throw new IllegalArgumentException(
                    "Challenge belongs to another session"
            );
        }

        if (tokenHash == null || !tokenHash.matches("^[0-9a-fA-F]{64}$")) {
            throw new IllegalArgumentException(
                    "Invalid step-up token hash"
            );
        }

        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after issuedAt"
            );
        }

        MfaStepUpToken token = new MfaStepUpToken();

        token.id = UuidV7Generator.generate();
        token.user = user;
        token.sessionId = sessionId;
        token.challenge = challenge;
        token.tokenHash = tokenHash.toLowerCase();
        token.createdAt = issuedAt;
        token.expiresAt = expiresAt;

        return token;
    }

    public boolean isExpired(Instant now) {
        Objects.requireNonNull(now);

        return !expiresAt.isAfter(now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isUsable(Instant now) {
        return !isRevoked() && !isExpired(now);
    }

    public void revoke(Instant now) {
        Objects.requireNonNull(now);

        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}