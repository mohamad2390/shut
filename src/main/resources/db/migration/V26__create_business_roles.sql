-- Business categories are roles; one account may hold several of them.
INSERT INTO roles (code, title, is_default, sort_order) VALUES
    ('STUDIO_OWNER', 'استودیودار', FALSE, 40),
    ('VENUE_OWNER', 'عمارت‌دار', FALSE, 50),
    ('EQUIPMENT_RENTER', 'اجاره‌دهندهٔ تجهیزات', FALSE, 60)
ON CONFLICT (code) DO NOTHING;

-- The basic card of a studio or an equipment rental, one per account and kind.
CREATE TABLE business_profiles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('STUDIO', 'RENTAL')),
    name VARCHAR(120) NOT NULL,
    city VARCHAR(100),
    description VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, kind)
);

-- Everyone who already owns a venue is a venue owner.
INSERT INTO user_roles (user_id, role_id)
SELECT DISTINCT v.owner_id, r.id FROM venues v CROSS JOIN roles r WHERE r.code = 'VENUE_OWNER'
ON CONFLICT DO NOTHING;
