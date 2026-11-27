-- An equipment rental shop: contact details, the items it rents out per day, and photos of each item.
CREATE TABLE rental_shops (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(20),
    city VARCHAR(100),
    address VARCHAR(300),
    description VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE rental_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES rental_shops(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    daily_price BIGINT NOT NULL CHECK (daily_price > 0),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_rental_items_shop ON rental_items(shop_id);

CREATE TABLE rental_item_photos (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES rental_items(id) ON DELETE CASCADE,
    file_id VARCHAR(512) NOT NULL,
    file_unique_id VARCHAR(64) NOT NULL,
    backup_message_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (item_id, file_unique_id)
);

-- Rental cards made with the simple business form move here; the owner completes the phone and address.
INSERT INTO rental_shops (owner_id, name, city, description)
SELECT user_id, name, city, description FROM business_profiles WHERE kind = 'RENTAL';
DROP TABLE business_profiles;
