ALTER TABLE addresses
    ADD COLUMN country_code CHAR(2) NULL AFTER line2,
    ADD COLUMN province_code CHAR(2) NULL AFTER country_code,
    ADD COLUMN ward_code CHAR(5) NULL AFTER province,
    MODIFY COLUMN district VARCHAR(120) NULL;

UPDATE addresses
SET country_code = 'VN'
WHERE country_code IS NULL;

ALTER TABLE addresses
    MODIFY COLUMN country_code CHAR(2) NOT NULL DEFAULT 'VN',
    ADD CONSTRAINT ck_addresses_country_supported
        CHECK (country_code = 'VN'),
    ADD CONSTRAINT ck_addresses_location_codes_pair
        CHECK (
            (province_code IS NULL AND ward_code IS NULL)
            OR
            (province_code IS NOT NULL AND ward_code IS NOT NULL)
        );
