CREATE TABLE addresses
(
    id BINARY(16) NOT NULL,

    user_id BINARY(16) NOT NULL,

    recipient VARCHAR(120) NOT NULL,

    phone VARCHAR(16) NOT NULL,

    line1 VARCHAR(255) NOT NULL,

    line2 VARCHAR(255) NULL,

    ward VARCHAR(120) NOT NULL,

    district VARCHAR(120) NOT NULL,

    province VARCHAR(120) NOT NULL,

    postal_code VARCHAR(12) NULL,

    is_default TINYINT(1)
        NOT NULL
        DEFAULT 0,

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    updated_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    deleted_at DATETIME(6) NULL,

    CONSTRAINT pk_addresses
        PRIMARY KEY (id),

    CONSTRAINT fk_addresses_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,

    CONSTRAINT ck_addresses_is_default
        CHECK (is_default IN (0, 1))

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE UNIQUE INDEX uk_addresses_user_id_id
    ON addresses (user_id, id);

CREATE INDEX ix_addresses_user_live_default
    ON addresses (
                  user_id,
                  deleted_at,
                  is_default
        );

CREATE INDEX ix_addresses_user_updated
    ON addresses (
                  user_id,
                  updated_at
        );