-- Every message the bot sent to a chat and every message a user sent to the bot, so that a chat can be cleaned up.
-- Telegram only lets a bot delete messages from the last 48 hours, so older rows are removed regularly.
CREATE TABLE chat_messages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_chat_messages_chat ON chat_messages (chat_id);
CREATE INDEX idx_chat_messages_created ON chat_messages (created_at);
