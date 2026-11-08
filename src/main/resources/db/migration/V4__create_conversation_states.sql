-- Where each Telegram user is in a multi-step bot conversation (for example, typing a new profile field).
CREATE TABLE conversation_states (
    telegram_id BIGINT PRIMARY KEY,
    step VARCHAR(50) NOT NULL,
    data JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
