CREATE TABLE shops
(
    id BINARY(16) NOT NULL,

    owner_user_id BINARY(16) NOT NULL,

    name VARCHAR(120) NOT NULL,

    slug VARCHAR(160) NOT NULL,

    business_name VARCHAR(200) NOT NULL,

    tax_code VARCHAR(20) NULL,

    description VARCHAR(2000) NULL,

    logo_object_key VARCHAR(512) NULL,

    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',

    kyc_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',

    warehouse_snapshot JSON NULL,

    bank_code VARCHAR(32) NULL,

    bank_name VARCHAR(120) NULL,

    bank_account_name VARCHAR(120) NULL,

    bank_account_last4 CHAR(4) NULL,

    bank_account_ciphertext VARBINARY(2048) NULL,

    bank_key_version VARCHAR(32) NULL,

    bank_verified_at DATETIME(6) NULL,

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    updated_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    deleted_at DATETIME(6) NULL,

    CONSTRAINT pk_shops
        PRIMARY KEY (id),

    CONSTRAINT fk_shops_owner
        FOREIGN KEY (owner_user_id)
        REFERENCES users(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT uk_shops_slug UNIQUE (slug),

    CONSTRAINT uk_shops_tax_code UNIQUE (tax_code),

    CONSTRAINT ck_shops_status
        CHECK (
            status IN (
                'DRAFT',
                'ACTIVE',
                'SUSPENDED',
                'DELETED'
            )
        ),

    CONSTRAINT ck_shops_kyc_status
        CHECK (
            kyc_status IN (
                'DRAFT',
                'PENDING',
                'NEEDS_INFO',
                'APPROVED',
                'REJECTED',
                'EXPIRED',
                'SUSPENDED'
            )
        ),

    CONSTRAINT ck_shops_bank_last4
        CHECK (
            bank_account_last4 IS NULL
                OR bank_account_last4 REGEXP '^[0-9]{4}$'
        )

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE INDEX ix_shops_owner_status
    ON shops (
         owner_user_id,
         status,
        deleted_at
    );

CREATE INDEX ix_shops_kyc_queue
    ON shops (
        kyc_status,
        status,
        updated_at
    );


CREATE TABLE seller_onboarding
(
    id BINARY(16) NOT NULL,

    shop_id BINARY(16) NOT NULL,

    current_step VARCHAR(24) NOT NULL DEFAULT 'PROFILE',

    profile_completed TINYINT(1) NOT NULL DEFAULT 0,

    kyc_completed TINYINT(1) NOT NULL DEFAULT 0,

    warehouse_completed TINYINT(1) NOT NULL DEFAULT 0,

    bank_completed TINYINT(1) NOT NULL DEFAULT 0,

    first_product_completed TINYINT(1) NOT NULL DEFAULT 0,

    blockers_json JSON NOT NULL DEFAULT (JSON_ARRAY()),

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    updated_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_seller_onboarding PRIMARY KEY (id),

    CONSTRAINT uk_seller_onboarding_shop UNIQUE (shop_id),

    CONSTRAINT fk_seller_onboarding_shop
        FOREIGN KEY (shop_id)
        REFERENCES shops(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT ck_seller_onboarding_step
        CHECK (
            current_step IN (
                'PROFILE',
                'KYC',
                'WAREHOUSE',
                'BANK',
                'FIRST_PRODUCT',
                'COMPLETED'
            )
        ),

    CONSTRAINT ck_onboarding_profile_completed
        CHECK (profile_completed IN (0, 1)),

    CONSTRAINT ck_onboarding_kyc_completed
        CHECK (kyc_completed IN (0, 1)),

    CONSTRAINT ck_onboarding_warehouse_completed
        CHECK (warehouse_completed IN (0, 1)),

    CONSTRAINT ck_onboarding_bank_completed
        CHECK (bank_completed IN (0, 1)),

    CONSTRAINT ck_onboarding_first_product_completed
        CHECK (first_product_completed IN (0, 1))

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE INDEX ix_onboarding_step_updated
    ON seller_onboarding (
        current_step,
        updated_at
    );