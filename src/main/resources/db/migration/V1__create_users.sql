CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    telegram_id BIGINT NOT NULL UNIQUE,
    telegram_username VARCHAR(100),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    user_type VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER'
        CHECK (user_type IN ('CUSTOMER', 'PROFESSIONAL', 'BOTH', 'ADMIN')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_telegram_username ON users (telegram_username);
CREATE INDEX idx_users_user_type ON users (user_type);
CREATE INDEX idx_users_active ON users (is_active);
