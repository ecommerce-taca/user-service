CREATE TABLE kyc_cases
(
    id BINARY(16) NOT NULL,

    shop_id BINARY(16) NOT NULL,

    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',

    submitted_at DATETIME(6) NULL,

    reviewed_by BINARY(16) NULL,

    reviewed_at DATETIME(6) NULL,

    decision_reason VARCHAR(1000) NULL,

    expires_at DATETIME(6) NULL,

    source_version INT UNSIGNED NOT NULL DEFAULT 1,

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    updated_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_kyc_cases PRIMARY KEY (id),

    CONSTRAINT fk_kyc_cases_shop
        FOREIGN KEY (shop_id)
        REFERENCES shops(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT ck_kyc_cases_status
        CHECK (
            status IN (
                'DRAFT',
                'PENDING',
                'NEEDS_INFO',
                'APPROVED',
                'REJECTED',
                'EXPIRED',
                'SUSPENDED'
            )
        ),

    CONSTRAINT ck_kyc_source_version
        CHECK (source_version >= 1),

    CONSTRAINT ck_kyc_review_state
        CHECK (
            reviewed_at IS NOT NULL
            OR reviewed_by IS NULL
        ),

    CONSTRAINT ck_kyc_decision_reason
        CHECK (
            decision_reason IS NULL
            OR CHAR_LENGTH(decision_reason)
            BETWEEN 10 AND 1000
        )

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_kyc_cases_shop_status
    ON kyc_cases (
        shop_id,
        status,
        updated_at
    );

CREATE INDEX ix_kyc_cases_queue
    ON kyc_cases (
        status,
        submitted_at
    );

CREATE INDEX ix_kyc_cases_expiry
    ON kyc_cases (
        expires_at,
        status
    );


CREATE TABLE kyc_documents
(
    id BINARY(16) NOT NULL,

    kyc_case_id BINARY(16) NOT NULL,

    document_type VARCHAR(32) NOT NULL,

    object_key VARCHAR(512) NOT NULL,

    original_file_name VARCHAR(255) NOT NULL,

    content_type VARCHAR(64) NOT NULL,

    size_bytes INT UNSIGNED NOT NULL,

    sha256 CHAR(64) NOT NULL,

    status VARCHAR(16) NOT NULL DEFAULT 'UPLOADING',

    uploaded_at DATETIME(6) NULL,

    verified_at DATETIME(6) NULL,

    deleted_at DATETIME(6) NULL,

    live_key TINYINT
        GENERATED ALWAYS AS (
            CASE
                WHEN deleted_at IS NULL
                    THEN 1
                ELSE NULL
            END
        ) STORED,

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    updated_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_kyc_documents
        PRIMARY KEY (id),

    CONSTRAINT fk_kyc_documents_case
        FOREIGN KEY (kyc_case_id)
        REFERENCES kyc_cases(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT uk_kyc_doc_checksum
        UNIQUE (
            kyc_case_id,
            document_type,
            sha256,
            live_key
        ),

    CONSTRAINT ck_kyc_document_status
        CHECK (
            status IN (
                'UPLOADING',
                'UPLOADED',
                'VERIFIED',
                'REJECTED',
                'EXPIRED',
                'DELETED'
            )
        ),

    CONSTRAINT ck_kyc_document_content_type
        CHECK (
            content_type IN (
                'application/pdf',
                'image/jpeg',
                'image/png'
            )
        ),

    CONSTRAINT ck_kyc_document_size
        CHECK (size_bytes BETWEEN 1 AND 10485760),

    CONSTRAINT ck_kyc_document_sha256
        CHECK (sha256 REGEXP '^[0-9A-Fa-f]{64}$')

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX ix_kyc_docs_case_status
    ON kyc_documents (
        kyc_case_id,
        status
    );