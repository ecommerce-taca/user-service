CREATE TABLE users
(
    id BINARY(16) NOT NULL,

    email VARCHAR(254) NOT NULL,

    email_normalized VARCHAR(254)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_0900_bin
        NOT NULL,

    email_verified_at DATETIME(6) NULL,

    phone VARCHAR(16) NULL,

    phone_normalized VARCHAR(16)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_0900_bin
        NULL,

    phone_verified_at DATETIME(6) NULL,

    password_hash VARCHAR(255) NOT NULL,

    full_name VARCHAR(120) NOT NULL,

    date_of_birth DATE NULL,

    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',

    failed_login_count SMALLINT UNSIGNED
        NOT NULL DEFAULT 0,

    failed_login_window_started_at DATETIME(6) NULL,

    locked_until DATETIME(6) NULL,

    password_changed_at DATETIME(6) NULL,

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    updated_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    deleted_at DATETIME(6) NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),

    CONSTRAINT uk_users_email_normalized
        UNIQUE (email_normalized),

    CONSTRAINT uk_users_phone_normalized
        UNIQUE (phone_normalized),

    CONSTRAINT ck_users_status
        CHECK (
            status IN (
                       'ACTIVE',
                       'LOCKED',
                       'SUSPENDED',
                       'DELETED'
                )
            ),

    CONSTRAINT ck_users_failed_login_count
        CHECK (failed_login_count >= 0)

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_users_status_created
    ON users (status, created_at);

CREATE INDEX ix_users_locked_until
    ON users (status, locked_until);