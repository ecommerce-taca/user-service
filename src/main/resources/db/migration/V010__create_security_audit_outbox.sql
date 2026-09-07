CREATE TABLE login_attempts
(
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    user_id BINARY(16) NULL,

    identifier_hash CHAR(64) NOT NULL,

    succeeded TINYINT(1) NOT NULL DEFAULT 0,

    failure_reason VARCHAR(32) NULL,

    ip_hash CHAR(64) NOT NULL,

    user_agent_hash CHAR(64) NULL,

    occurred_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_login_attempts PRIMARY KEY (id),

    CONSTRAINT fk_login_attempts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT ck_login_attempt_succeeded
        CHECK (succeeded IN (0, 1)),

    CONSTRAINT ck_login_attempt_success_reason
        CHECK (
            succeeded = 0
            OR failure_reason IS NULL
        )

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE INDEX ix_login_identifier_time
    ON login_attempts (
        identifier_hash,
        occurred_at
    );


CREATE INDEX ix_login_user_time
    ON login_attempts (
        user_id,
        occurred_at
    );


CREATE TABLE audit_logs
(
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    event_id BINARY(16) NOT NULL,

    actor_user_id BINARY(16) NULL,

    action VARCHAR(64) NOT NULL,

    target_type VARCHAR(32) NOT NULL,

    target_id BINARY(16) NULL,

    reason VARCHAR(1000) NULL,

    metadata JSON NOT NULL DEFAULT (JSON_OBJECT()),

    ip_hash CHAR(64) NULL,

    occurred_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_audit_logs PRIMARY KEY (id),

    CONSTRAINT uk_audit_event_id UNIQUE (event_id),

    CONSTRAINT ck_audit_target_type
        CHECK (
            target_type IN (
                'USER',
                'SHOP',
                'KYC',
                'ROLE'
            )
        )

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE INDEX ix_audit_target_time
    ON audit_logs (
        target_type,
        target_id,
        occurred_at
    );


CREATE INDEX ix_audit_actor_time
    ON audit_logs (
        actor_user_id,
        occurred_at
    );


CREATE TABLE outbox_events
(
    id BINARY(16) NOT NULL,

    aggregate_type VARCHAR(32) NOT NULL,

    aggregate_id BINARY(16) NOT NULL,

    event_type VARCHAR(64) NOT NULL,

    schema_version SMALLINT UNSIGNED NOT NULL DEFAULT 1,

    partition_key VARCHAR(128) NOT NULL,

    payload JSON NOT NULL,

    attempt_count TINYINT UNSIGNED NOT NULL DEFAULT 0,

    next_retry_at DATETIME(6) NULL,

    published_at DATETIME(6) NULL,

    failed_at DATETIME(6) NULL,

    last_error_code VARCHAR(64) NULL,

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_outbox_events PRIMARY KEY (id),

    CONSTRAINT ck_outbox_aggregate_type
        CHECK (
            aggregate_type IN (
                'USER',
                'SHOP',
                'KYC'
            )
        ),

    CONSTRAINT ck_outbox_schema_version
        CHECK (schema_version >= 1),

    CONSTRAINT ck_outbox_attempt_count
        CHECK (attempt_count BETWEEN 0 AND 3),

    CONSTRAINT ck_outbox_terminal_state
        CHECK (
            published_at IS NULL
            OR failed_at IS NULL
        )

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE INDEX ix_outbox_pending
    ON outbox_events (
        published_at,
        failed_at,
        next_retry_at,
        created_at
    );


CREATE INDEX ix_outbox_aggregate
    ON outbox_events (
        aggregate_type,
        aggregate_id,
        created_at
    );