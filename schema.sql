CREATE TABLE IF NOT EXISTS users
(
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    phone_number  VARCHAR(13)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    created_at    BIGINT       NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);
