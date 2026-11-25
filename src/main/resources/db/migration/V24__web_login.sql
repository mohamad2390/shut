-- Web accounts: a person can sign in from the web app with a phone number and a one-time code.
-- A web-only account has no Telegram id yet; it gets one when the same person uses the bot.
ALTER TABLE users ALTER COLUMN telegram_id DROP NOT NULL;

-- One account per phone number, so signing in by phone always finds the same person.
CREATE UNIQUE INDEX ux_users_phone_number ON users (phone_number) WHERE phone_number IS NOT NULL;

-- One-time codes sent by SMS. Only a hash of the code is kept.
CREATE TABLE otp_codes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_otp_codes_phone ON otp_codes (phone, id DESC);

-- Web sign-ins. The token itself is only given to the browser; we keep its hash.
CREATE TABLE web_sessions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_web_sessions_user ON web_sessions (user_id);
