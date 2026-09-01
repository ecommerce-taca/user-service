CREATE TABLE two_factor_credentials
(
    id BINARY(16) NOT NULL,

    user_id BINARY(16) NOT NULL,

    secret_ciphertext VARBINARY(1024) NOT NULL,

    key_version VARCHAR(32) NOT NULL,

    status VARCHAR(16) NOT NULL DEFAULT 'ENROLLING',

    enabled_at DATETIME(6) NULL,

    disabled_at DATETIME(6) NULL,

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    updated_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_two_factor_credentials PRIMARY KEY (id),

    CONSTRAINT uk_two_factor_user UNIQUE (user_id),

    CONSTRAINT fk_two_factor_credentials_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT ck_two_factor_status
        CHECK (
            status IN (
                'DISABLED',
                'ENROLLING',
                'ENABLED',
                'RESET_REQUIRED'
            )
        ),

    CONSTRAINT ck_two_factor_enabled_state
        CHECK (
            status <> 'ENABLED'
            OR enabled_at IS NOT NULL
        ),

    CONSTRAINT ck_two_factor_disabled_state
        CHECK (
            status <> 'DISABLED'
            OR disabled_at IS NOT NULL
        )

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE TABLE two_factor_recovery_codes
(
    id BINARY(16) NOT NULL,

    credential_id BINARY(16) NOT NULL,

    code_hash CHAR(64) NOT NULL,

    used_at DATETIME(6) NULL,

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_two_factor_recovery_codes PRIMARY KEY (id),

    CONSTRAINT fk_recovery_codes_credential
        FOREIGN KEY (credential_id)
        REFERENCES two_factor_credentials(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT uk_recovery_code_hash UNIQUE (code_hash),

    CONSTRAINT ck_recovery_code_hash_format
        CHECK (code_hash REGEXP '^[0-9A-Fa-f]{64}$')

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE INDEX ix_recovery_credential_used
    ON two_factor_recovery_codes (
        credential_id,
        used_at
    );


CREATE TABLE mfa_challenges
(
    id BINARY(16) NOT NULL,

    user_id BINARY(16) NOT NULL,

    purpose VARCHAR(16) NOT NULL,

    code_hash CHAR(64) NULL,

    expires_at DATETIME(6) NOT NULL,

    attempt_count TINYINT UNSIGNED NOT NULL DEFAULT 0,

    verified_at DATETIME(6) NULL,

    revoked_at DATETIME(6) NULL,

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_mfa_challenges PRIMARY KEY (id),

    CONSTRAINT fk_mfa_challenges_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT ck_mfa_challenge_purpose
        CHECK (
            purpose IN (
                'LOGIN',
                'STEP_UP',
                'ENROLL'
            )
        ),

    CONSTRAINT ck_mfa_challenge_attempt_count
        CHECK (attempt_count BETWEEN 0 AND 5),

    CONSTRAINT ck_mfa_challenge_expiry
        CHECK (expires_at > created_at),

    CONSTRAINT ck_mfa_challenge_code_hash
        CHECK (
            code_hash IS NULL
            OR code_hash REGEXP '^[0-9A-Fa-f]{64}$'
        ),

    CONSTRAINT ck_mfa_challenge_verified_state
        CHECK (
            verified_at IS NULL
            OR revoked_at IS NULL
        )

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE INDEX ix_mfa_challenge_user_expiry
    ON mfa_challenges (
        user_id,
        purpose,
        expires_at,
        verified_at
    );