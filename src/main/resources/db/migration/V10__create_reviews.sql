-- One rating (1 to 5 stars) and an optional comment from a member about another member.
CREATE TABLE reviews (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reviewer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (provider_id <> reviewer_id),
    UNIQUE (provider_id, reviewer_id)
);

CREATE INDEX idx_reviews_provider_created ON reviews (provider_id, created_at DESC);
