-- Portfolio items. The media itself stays on Telegram; only its file ids are stored here.
CREATE TABLE showcase_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(30) NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    file_id VARCHAR(512) NOT NULL,
    file_unique_id VARCHAR(64) NOT NULL,
    caption VARCHAR(1024),
    backup_message_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, file_unique_id)
);

CREATE INDEX idx_showcase_items_user_category ON showcase_items(user_id, category, created_at DESC);
