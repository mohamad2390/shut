-- Which member's prices a price widget message shows. Telegram sends the bot only the message id
-- when someone taps inside a widget, so the bot has to remember what each widget message is about.
CREATE TABLE price_widgets (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (chat_id, message_id)
);
