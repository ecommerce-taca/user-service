ALTER TABLE verification_tokens
    ADD COLUMN recipient_value VARCHAR(254) NULL
        AFTER recipient_masked;