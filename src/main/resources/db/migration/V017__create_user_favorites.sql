CREATE TABLE user_favorites
(
    user_id BINARY(16) NOT NULL,

    product_id BINARY(16) NOT NULL,

    created_at DATETIME(6)
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_user_favorites
        PRIMARY KEY (
            user_id,
            product_id
        ),

    CONSTRAINT fk_user_favorites_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT

) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE INDEX ix_user_favorites_user_created
    ON user_favorites (
        user_id,
        created_at,
        product_id
    );