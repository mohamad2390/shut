-- The services a member can pick from when adding a price, each with an icon the bot shows next to it.
-- Change an icon any time with: UPDATE service_types SET icon = '🌅' WHERE name = '...';
CREATE TABLE service_types (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE,
    icon VARCHAR(16) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- The icon shown for each pricing unit (the codes are the values of the PricingUnit enum).
-- Change one any time with: UPDATE pricing_unit_icons SET icon = '🕐' WHERE unit = 'HOUR';
CREATE TABLE pricing_unit_icons (
    unit VARCHAR(20) PRIMARY KEY,
    icon VARCHAR(16) NOT NULL
);

-- Prices added from the catalog point to their service. Older prices (typed by hand) have no service.
-- If a service is deleted, its prices keep their title.
ALTER TABLE price_items ADD COLUMN service_type_id BIGINT REFERENCES service_types(id) ON DELETE SET NULL;

INSERT INTO service_types (name, icon, sort_order) VALUES
    ('عکاسی پرتره', '📷', 10),
    ('عکاسی عروسی و عقد', '💍', 20),
    ('عکاسی مراسم و مهمانی', '🎉', 30),
    ('عکاسی کودک و بارداری', '👶', 40),
    ('عکاسی محصول', '🛍', 50),
    ('عکاسی صنعتی و تبلیغاتی', '🏭', 60),
    ('عکاسی معماری و ملک', '🏠', 70),
    ('عکاسی و فیلمبرداری هوایی', '🚁', 80),
    ('فیلمبرداری مراسم', '🎥', 90),
    ('فیلمبرداری تبلیغاتی', '🎬', 100),
    ('تیزر و کلیپ', '🎞', 110),
    ('ادیت و روتوش عکس', '🖼', 120),
    ('تدوین ویدیو', '✂️', 130),
    ('رنگ‌بندی ویدیو', '🎨', 140),
    ('موشن گرافیک', '✨', 150),
    ('طراحی آلبوم', '📖', 160),
    ('اجاره استودیو', '🏢', 170),
    ('اجاره تجهیزات', '🎒', 180),
    ('سایر خدمات', '🧩', 1000);

INSERT INTO pricing_unit_icons (unit, icon) VALUES
    ('HOUR', '⏱'),
    ('DAY', '📅'),
    ('PHOTO', '📷'),
    ('VIDEO_MINUTE', '🎬'),
    ('PROJECT', '📦');
