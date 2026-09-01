CREATE TABLE IF NOT EXISTS users
(
    id
               BIGINT
        AUTO_INCREMENT
        PRIMARY
            KEY,
    username
               VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles
(
    user_id
        BIGINT
                    NOT
                        NULL,
    role
        VARCHAR(50) NOT NULL,
    PRIMARY KEY
        (
         user_id,
         role
            ),
    CONSTRAINT fk_user_roles_user FOREIGN KEY
        (
         user_id
            ) REFERENCES users
            (
             id
                ) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refresh_tokens
(
    id
                BIGINT
        AUTO_INCREMENT
        PRIMARY
            KEY,
    user_id
                BIGINT
                             NOT
                                 NULL,
    token
                VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY
        (
         user_id
            ) REFERENCES users
            (
             id
                ) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);