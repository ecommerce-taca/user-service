ALTER TABLE shops
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0
        AFTER kyc_status;


ALTER TABLE shops
    ADD CONSTRAINT ck_shops_version
        CHECK (version >= 0);