CREATE TABLE roles
(
    id BINARY(16) NOT NULL,

    role_key VARCHAR(32) NOT NULL,

    scope_type VARCHAR(16) NOT NULL DEFAULT 'USER',

    description VARCHAR(255) NOT NULL,

    is_system TINYINT(1) NOT NULL DEFAULT 1,

    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    updated_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_roles PRIMARY KEY (id),

    CONSTRAINT uk_roles_key UNIQUE (role_key),

    CONSTRAINT ck_roles_scope_type
        CHECK (
            scope_type IN (
                    'USER',
                    'SHOP',
                    'SYSTEM'
                )
            ),

    CONSTRAINT ck_roles_is_system
        CHECK ( is_system IN (0, 1) )

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE TABLE permissions
(
    id BINARY(16) NOT NULL,

    permission_key VARCHAR(64) NOT NULL,

    scope_type VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',

    description VARCHAR(255) NOT NULL,

    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_permissions PRIMARY KEY (id),

    CONSTRAINT uk_permissions_key UNIQUE (permission_key),

    CONSTRAINT ck_permissions_scope_type
        CHECK (
            scope_type IN (
                    'USER',
                    'SHOP',
                    'SYSTEM'
                )
            )

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE TABLE role_permissions
(
    role_id BINARY(16) NOT NULL,

    permission_id BINARY(16) NOT NULL,

    granted_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    granted_by BINARY(16) NULL,

    CONSTRAINT pk_role_permissions
        PRIMARY KEY (
                role_id,
                permission_id
            ),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
            REFERENCES roles(id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT,

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
            REFERENCES permissions(id)
            ON UPDATE RESTRICT
            ON DELETE RESTRICT

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;