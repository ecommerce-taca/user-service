CREATE TABLE user_roles
(
    id BINARY(16) NOT NULL,

    user_id BINARY(16) NOT NULL,

    role_id BINARY(16) NOT NULL,

    shop_id BINARY(16) NULL,

    scope_key BINARY(16)
        GENERATED ALWAYS AS (
            COALESCE(
                shop_id,
                X'00000000000000000000000000000000'
            )
        ) STORED,

    granted_by BINARY(16) NULL,

    granted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    revoked_at DATETIME(6) NULL,

    updated_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_user_roles PRIMARY KEY (id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT fk_user_roles_shop
        FOREIGN KEY (shop_id)
        REFERENCES shops(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT uk_user_role_scope
        UNIQUE (
            user_id,
            role_id,
            scope_key
        )

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE INDEX ix_user_roles_user_active
    ON user_roles (
        user_id,
        revoked_at
    );


CREATE INDEX ix_user_roles_shop_active
    ON user_roles (
        shop_id,
         revoked_at
    );


CREATE TABLE shop_staff
(
    id BINARY(16) NOT NULL,

    shop_id BINARY(16) NOT NULL,

    user_id BINARY(16) NOT NULL,

    staff_role VARCHAR(32) NOT NULL,

    status VARCHAR(16) NOT NULL DEFAULT 'INVITED',

    invited_by BINARY(16) NOT NULL,

    invited_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    joined_at DATETIME(6) NULL,

    revoked_at DATETIME(6) NULL,

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    updated_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_shop_staff
        PRIMARY KEY (id),

    CONSTRAINT fk_shop_staff_shop
        FOREIGN KEY (shop_id)
        REFERENCES shops(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT fk_shop_staff_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT fk_shop_staff_invited_by
        FOREIGN KEY (invited_by)
        REFERENCES users(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT uk_shop_staff_member
        UNIQUE (
            shop_id,
            user_id
        ),

    CONSTRAINT ck_shop_staff_status
        CHECK (
            status IN (
                'INVITED',
                'ACTIVE',
                'REVOKED'
            )
        )

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE INDEX ix_shop_staff_user_status
    ON shop_staff (
        user_id,
        status
    );