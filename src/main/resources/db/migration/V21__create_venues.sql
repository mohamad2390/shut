-- Places that are rented out for photography: mansions, gardens, studios. Anyone in the channel can list one.
-- A venue shows up in the list once it has a city and a price and is active.
CREATE TABLE venues (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    venue_type VARCHAR(20),
    city VARCHAR(100),
    address VARCHAR(300),
    description VARCHAR(1000),
    amenities VARCHAR(500),
    price_amount BIGINT,
    price_unit VARCHAR(20),
    phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_venues_owner ON venues (owner_id);
CREATE INDEX idx_venues_listing ON venues (active, id DESC);

-- Photos are kept as Telegram file ids, like the showcase.
CREATE TABLE venue_photos (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id BIGINT NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    file_id VARCHAR(512) NOT NULL,
    file_unique_id VARCHAR(64) NOT NULL,
    backup_message_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_venue_photos UNIQUE (venue_id, file_unique_id)
);

-- Which days the venue is free or taken, the same way a specialist's calendar works.
CREATE TABLE venue_calendar_days (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id BIGINT NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    day_date DATE NOT NULL,
    status VARCHAR(10) NOT NULL,
    CONSTRAINT uq_venue_calendar_day UNIQUE (venue_id, day_date)
);

-- A request to use a venue on a day. The owner accepts or declines; accepting marks the day taken.
CREATE TABLE venue_bookings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id BIGINT NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day_date DATE NOT NULL,
    note VARCHAR(500),
    status VARCHAR(12) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_venue_bookings_venue ON venue_bookings (venue_id, status);
CREATE INDEX idx_venue_bookings_requester ON venue_bookings (requester_id, id DESC);
CREATE UNIQUE INDEX uq_venue_bookings_pending ON venue_bookings (venue_id, requester_id, day_date) WHERE status = 'PENDING';
