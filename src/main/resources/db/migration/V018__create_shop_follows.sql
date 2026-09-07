CREATE TABLE shop_follows
(
    user_id BINARY(16) NOT NULL,

    shop_id BINARY(16) NOT NULL,

    followed_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_shop_follows
        PRIMARY KEY (
            user_id,
            shop_id
        ),

    CONSTRAINT fk_shop_follows_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT fk_shop_follows_shop
        FOREIGN KEY (shop_id)
        REFERENCES shops(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE INDEX ix_shop_follows_user_followed
    ON shop_follows (
        user_id,
        followed_at,
        shop_id
    );


CREATE INDEX ix_shop_follows_shop_user
    ON shop_follows (
        shop_id,
        user_id
    );