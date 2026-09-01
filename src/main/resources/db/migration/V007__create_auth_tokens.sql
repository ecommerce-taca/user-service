CREATE TABLE refresh_tokens
(
    id BINARY(16) NOT NULL,

    user_id BINARY(16) NOT NULL,

    token_hash CHAR(64) NOT NULL,

    family_id BINARY(16) NOT NULL,

    issued_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    expires_at DATETIME(6) NOT NULL,

    revoked_at DATETIME(6) NULL,

    revoke_reason VARCHAR(32) NULL,

    replaced_by_token_id BINARY(16) NULL,

    last_seen_at DATETIME(6) NULL,

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),

    CONSTRAINT ck_refresh_token_expiry
        CHECK (expires_at > issued_at),

    CONSTRAINT ck_refresh_revoke_reason
        CHECK (
            revoke_reason IS NULL
            OR revoke_reason IN (
                'ROTATED',
                'SIGNOUT',
                'RESET',
                'REUSE',
                'SUSPEND',
                'EXPIRED'
            )
        ),

    CONSTRAINT ck_refresh_revoke_state
        CHECK (
            revoked_at IS NOT NULL
            OR revoke_reason IS NULL
        )

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_refresh_user_active
    ON refresh_tokens (
        user_id,
        revoked_at,
        expires_at
    );

CREATE INDEX ix_refresh_family
    ON refresh_tokens (
        family_id,
        revoked_at
    );

CREATE INDEX ix_refresh_expiry
    ON refresh_tokens (
        expires_at
    );


CREATE TABLE verification_tokens
(
    id BINARY(16) NOT NULL,

    user_id BINARY(16) NOT NULL,

    channel VARCHAR(8) NOT NULL,

    purpose VARCHAR(32) NOT NULL,

    token_hash CHAR(64) NOT NULL,

    recipient_masked VARCHAR(254) NOT NULL,

    expires_at DATETIME(6) NOT NULL,

    used_at DATETIME(6) NULL,

    revoked_at DATETIME(6) NULL,

    attempt_count TINYINT UNSIGNED
        NOT NULL
        DEFAULT 0,

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_verification_tokens
        PRIMARY KEY (id),

    CONSTRAINT fk_verification_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT ck_verification_channel
        CHECK (
            channel IN (
                'EMAIL',
                'PHONE'
            )
        ),

    CONSTRAINT ck_verification_purpose
        CHECK (
            purpose IN (
                'EMAIL_VERIFY',
                'PHONE_VERIFY'
            )
        ),

    CONSTRAINT ck_verification_attempt_count
        CHECK (attempt_count BETWEEN 0 AND 5),

    CONSTRAINT ck_verification_expiry
        CHECK (expires_at > created_at)

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_verification_token_hash
    ON verification_tokens (
        token_hash,
        used_at,
        revoked_at
    );

CREATE INDEX ix_verification_user_purpose
    ON verification_tokens (
        user_id,
        purpose,
        channel,
        created_at
    );

CREATE INDEX ix_verification_expiry
    ON verification_tokens (
        expires_at
    );


CREATE TABLE password_reset_tokens
(
    id BINARY(16) NOT NULL,

    user_id BINARY(16) NOT NULL,

    token_hash CHAR(64) NOT NULL,

    expires_at DATETIME(6) NOT NULL,

    used_at DATETIME(6) NULL,

    revoked_at DATETIME(6) NULL,

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),

    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT uk_password_reset_token_hash
        UNIQUE (token_hash),

    CONSTRAINT ck_password_reset_expiry
        CHECK (expires_at > created_at)

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE INDEX ix_password_reset_user_active
    ON password_reset_tokens (
        user_id,
        used_at,
        revoked_at,
        expires_at
    );