ALTER TABLE mfa_challenges
    ADD COLUMN session_id BINARY(16) NULL AFTER user_id;

CREATE INDEX ix_mfa_challenge_user_purpose_session
    ON mfa_challenges (
        user_id,
        purpose,
        session_id,
        expires_at,
        verified_at,
        revoked_at
    );

CREATE TABLE mfa_step_up_tokens (
    id BINARY(16) NOT NULL,

    user_id BINARY(16) NOT NULL,

    session_id BINARY(16) NOT NULL,

    challenge_id BINARY(16) NOT NULL,

    token_hash CHAR(64) NOT NULL,

    expires_at DATETIME(6) NOT NULL,

    revoked_at DATETIME(6) NULL,

    created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_mfa_step_up_tokens PRIMARY KEY (id),

    CONSTRAINT uq_mfa_step_up_token_hash UNIQUE (token_hash),

    CONSTRAINT uq_mfa_step_up_challenge UNIQUE (challenge_id),

    CONSTRAINT fk_mfa_step_up_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_mfa_step_up_challenge
        FOREIGN KEY (challenge_id)
        REFERENCES mfa_challenges(id),

    INDEX ix_mfa_step_up_user_session (
        user_id,
        session_id,
        expires_at,
        revoked_at
    )
);