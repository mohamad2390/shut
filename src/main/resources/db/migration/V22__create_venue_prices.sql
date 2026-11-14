-- A venue can have several prices: per hour, per half day, per day, or a named one such as "private" (a fixed price).
CREATE TABLE venue_prices (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id BIGINT NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    title VARCHAR(40),
    unit VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_venue_prices_venue ON venue_prices (venue_id);

-- The single price every venue had before moves over as its first price.
INSERT INTO venue_prices (venue_id, unit, amount)
SELECT id, price_unit, price_amount FROM venues WHERE price_amount IS NOT NULL AND price_unit IS NOT NULL;

ALTER TABLE venues DROP COLUMN price_amount, DROP COLUMN price_unit;
