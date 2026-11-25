-- A studio is its own thing, not a business card: contact details, services with prices, a showcase and people.
CREATE TABLE studios (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    name VARCHAR(120) NOT NULL,
    manager_name VARCHAR(100),
    phone VARCHAR(20),
    city VARCHAR(100),
    address VARCHAR(300),
    description VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- A service is a package (e.g. a full wedding: photography, video, formalities, album) with an optional price.
CREATE TABLE studio_offerings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    studio_id BIGINT NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    includes VARCHAR(500),
    price BIGINT CHECK (price IS NULL OR price > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_studio_offerings_studio ON studio_offerings(studio_id);

CREATE TABLE studio_media (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    studio_id BIGINT NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('PHOTO', 'VIDEO')),
    file_id VARCHAR(512) NOT NULL,
    file_unique_id VARCHAR(64) NOT NULL,
    backup_message_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (studio_id, file_unique_id)
);

-- A specialist joins only after accepting the invitation.
CREATE TABLE studio_members (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    studio_id BIGINT NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(10) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (studio_id, user_id)
);
CREATE INDEX idx_studio_members_user ON studio_members(user_id);

-- Studio cards made with the earlier business form move here; they are listed once their details are complete.
INSERT INTO studios (owner_id, name, city, description)
SELECT user_id, name, city, description FROM business_profiles WHERE kind = 'STUDIO';
DELETE FROM business_profiles WHERE kind = 'STUDIO';
