ALTER TABLE outbox_events
    ADD COLUMN actor_user_id BINARY(16) NULL
        AFTER aggregate_id;


CREATE INDEX ix_outbox_actor_user
    ON outbox_events (
        actor_user_id,
        created_at
    );