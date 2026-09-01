CREATE INDEX ix_login_attempts_occurred
    ON login_attempts (
        occurred_at
    );

CREATE INDEX ix_audit_occurred
    ON audit_logs (
       occurred_at
    );