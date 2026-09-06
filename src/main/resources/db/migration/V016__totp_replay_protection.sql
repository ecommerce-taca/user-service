ALTER TABLE two_factor_credentials
    ADD COLUMN last_totp_step BIGINT NULL
        AFTER disabled_at;