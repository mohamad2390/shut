-- A reference list of the gear of the industry, built from three tables that together make one product:
--   type (camera) + brand (Sony) + model (Alpha 7) = one unique product.
-- What members add that is not in the list is added to it, marked verified = FALSE until someone reviews it:
--   UPDATE equipment_models SET verified = TRUE WHERE id = ...;
-- normalized_name is the name without spaces, dashes, upper case or letter/digit variants, and is what must be unique,
-- so "Alpha 7", "alpha-7" and "آلفا ۷" typed the same way never become two products.
CREATE TABLE equipment_types (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    normalized_name VARCHAR(80) NOT NULL UNIQUE,
    icon VARCHAR(16) NOT NULL,
    sort_order INT NOT NULL DEFAULT 1000,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE equipment_brands (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    normalized_name VARCHAR(80) NOT NULL UNIQUE,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Other spellings of a brand, so "Sony" finds "سونی". Add more any time:
--   INSERT INTO equipment_brand_aliases (brand_id, alias, normalized_alias) SELECT id, 'Sony Corp', 'sonycorp' FROM equipment_brands WHERE name = 'سونی';
CREATE TABLE equipment_brand_aliases (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    brand_id BIGINT NOT NULL REFERENCES equipment_brands(id) ON DELETE CASCADE,
    alias VARCHAR(80) NOT NULL,
    normalized_alias VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE equipment_models (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type_id BIGINT NOT NULL REFERENCES equipment_types(id),
    brand_id BIGINT NOT NULL REFERENCES equipment_brands(id),
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_equipment_models_product UNIQUE (type_id, brand_id, normalized_name)
);

CREATE INDEX idx_equipment_models_brand ON equipment_models (brand_id);

-- The gear a member owns: one row per product.
CREATE TABLE user_equipment (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    model_id BIGINT NOT NULL REFERENCES equipment_models(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_equipment UNIQUE (user_id, model_id)
);

CREATE INDEX idx_user_equipment_user ON user_equipment (user_id);

INSERT INTO equipment_types (name, normalized_name, icon, sort_order, verified) VALUES
    ('دوربین عکاسی', 'دوربینعکاسی', '📷', 10, TRUE),
    ('دوربین فیلمبرداری', 'دوربینفیلمبرداری', '🎥', 20, TRUE),
    ('لنز', 'لنز', '🔭', 30, TRUE),
    ('پهپاد', 'پهپاد', '🚁', 40, TRUE),
    ('گیمبال و استابلایزر', 'گیمبالواستابلایزر', '🎬', 50, TRUE),
    ('سه‌پایه و مونوپاد', 'سهپایهومونوپاد', '📐', 60, TRUE),
    ('نور و فلاش', 'نوروفلاش', '💡', 70, TRUE),
    ('میکروفون و صدا', 'میکروفونوصدا', '🎙', 80, TRUE),
    ('مانیتور و مانیتورینگ', 'مانیتورومانیتورینگ', '🖥', 90, TRUE),
    ('کارت حافظه', 'کارتحافظه', '💾', 100, TRUE),
    ('باتری و شارژر', 'باتریوشارژر', '🔋', 110, TRUE),
    ('فیلتر', 'فیلتر', '🔘', 120, TRUE),
    ('ریل و کرین', 'ریلوکرین', '🎞', 130, TRUE),
    ('اکشن‌کم', 'اکشنکم', '⚡', 140, TRUE),
    ('کامپیوتر و لپ‌تاپ', 'کامپیوترولپتاپ', '💻', 150, TRUE),
    ('چاپگر عکس', 'چاپگرعکس', '🖨', 160, TRUE);

INSERT INTO equipment_brands (name, normalized_name, verified) VALUES
    ('سونی', 'سونی', TRUE), ('کانن', 'کانن', TRUE), ('نیکون', 'نیکون', TRUE),
    ('فوجی‌فیلم', 'فوجیفیلم', TRUE), ('پاناسونیک', 'پاناسونیک', TRUE), ('بلک‌مجیک', 'بلکمجیک', TRUE),
    ('دی‌جی‌آی', 'دیجیآی', TRUE), ('رد', 'رد', TRUE), ('آری', 'آری', TRUE),
    ('سیگما', 'سیگما', TRUE), ('تامرون', 'تامرون', TRUE), ('زایس', 'زایس', TRUE),
    ('گودکس', 'گودکس', TRUE), ('اپوچر', 'اپوچر', TRUE), ('رود', 'رود', TRUE),
    ('سنهایزر', 'سنهایزر', TRUE), ('ژیون', 'ژیون', TRUE), ('مانفروتو', 'مانفروتو', TRUE),
    ('سن‌دیسک', 'سندیسک', TRUE), ('گوپرو', 'گوپرو', TRUE), ('لایکا', 'لایکا', TRUE),
    ('المپوس', 'المپوس', TRUE), ('هاسلبلاد', 'هاسلبلاد', TRUE), ('اینستا۳۶۰', 'اینستا360', TRUE),
    ('پروفوتو', 'پروفوتو', TRUE);

INSERT INTO equipment_brand_aliases (brand_id, alias, normalized_alias)
SELECT b.id, a.alias, a.normalized_alias
FROM (VALUES
    ('سونی', 'Sony', 'sony'), ('کانن', 'Canon', 'canon'), ('نیکون', 'Nikon', 'nikon'),
    ('فوجی‌فیلم', 'Fujifilm', 'fujifilm'), ('فوجی‌فیلم', 'فوجی', 'فوجی'),
    ('پاناسونیک', 'Panasonic', 'panasonic'), ('بلک‌مجیک', 'Blackmagic', 'blackmagic'),
    ('بلک‌مجیک', 'Blackmagic Design', 'blackmagicdesign'), ('دی‌جی‌آی', 'DJI', 'dji'),
    ('رد', 'RED', 'red'), ('آری', 'ARRI', 'arri'), ('سیگما', 'Sigma', 'sigma'),
    ('تامرون', 'Tamron', 'tamron'), ('زایس', 'Zeiss', 'zeiss'), ('گودکس', 'Godox', 'godox'),
    ('اپوچر', 'Aputure', 'aputure'), ('رود', 'Rode', 'rode'), ('سنهایزر', 'Sennheiser', 'sennheiser'),
    ('ژیون', 'Zhiyun', 'zhiyun'), ('مانفروتو', 'Manfrotto', 'manfrotto'), ('سن‌دیسک', 'SanDisk', 'sandisk'),
    ('گوپرو', 'GoPro', 'gopro'), ('لایکا', 'Leica', 'leica'), ('المپوس', 'Olympus', 'olympus'),
    ('هاسلبلاد', 'Hasselblad', 'hasselblad'), ('اینستا۳۶۰', 'Insta360', 'insta360'),
    ('پروفوتو', 'Profoto', 'profoto')
) AS a(name, alias, normalized_alias)
JOIN equipment_brands b ON b.name = a.name;
