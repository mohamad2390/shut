-- Members sell their own gear: a listing, its sample photos (kept as Telegram file ids, like venue photos) and the
-- offers buyers make on it. An offer without an amount means "I'll take it at the asking price".
CREATE TABLE listings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    seller_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    category_id BIGINT REFERENCES equipment_types(id),
    brand VARCHAR(60),
    model VARCHAR(80),
    item_condition VARCHAR(20) NOT NULL,
    price BIGINT NOT NULL,
    negotiable BOOLEAN NOT NULL DEFAULT TRUE,
    city VARCHAR(100),
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listings_seller ON listings (seller_id);
CREATE INDEX idx_listings_public ON listings (status, hidden, id DESC);

CREATE TABLE listing_photos (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    file_id VARCHAR(512) NOT NULL,
    file_unique_id VARCHAR(64) NOT NULL,
    backup_message_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listing_photos_listing ON listing_photos (listing_id);

CREATE TABLE listing_offers (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    buyer_id BIGINT NOT NULL REFERENCES users(id),
    amount BIGINT,
    message VARCHAR(500),
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    answered_at TIMESTAMPTZ
);

CREATE INDEX idx_listing_offers_listing ON listing_offers (listing_id, status);
CREATE INDEX idx_listing_offers_buyer ON listing_offers (buyer_id, id DESC);
