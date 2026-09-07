ALTER TABLE mfa_challenges
    ADD COLUMN login_identifier_hash CHAR(64) NULL
        AFTER session_id,

    ADD COLUMN login_ip_hash CHAR(64) NULL
        AFTER login_identifier_hash,

    ADD COLUMN login_user_agent_hash CHAR(64) NULL
        AFTER login_ip_hash;