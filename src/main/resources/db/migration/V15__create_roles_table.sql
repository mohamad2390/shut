-- Roles become rows of their own, so a new role is an INSERT and not a code change.
-- user_roles used to hold the role as text with a CHECK on three values; it now points to a row of roles.
CREATE TABLE roles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    title VARCHAR(60) NOT NULL,
    -- Every new account gets the roles marked here.
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0
);

INSERT INTO roles (code, title, is_default, sort_order) VALUES
    ('CUSTOMER', 'کاربر عادی', TRUE, 10),
    ('PROVIDER', 'متخصص', FALSE, 20),
    ('ADMIN', 'مدیر', FALSE, 30);

ALTER TABLE user_roles ADD COLUMN role_id BIGINT REFERENCES roles(id);
UPDATE user_roles ur SET role_id = r.id FROM roles r WHERE r.code = ur.role;
ALTER TABLE user_roles ALTER COLUMN role_id SET NOT NULL;

ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS user_roles_pkey;
DROP INDEX IF EXISTS idx_user_roles_role;
ALTER TABLE user_roles DROP COLUMN role;
ALTER TABLE user_roles ADD PRIMARY KEY (user_id, role_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- Everyone who already has a profile is a specialist.
INSERT INTO user_roles (user_id, role_id)
SELECT p.user_id, r.id FROM professional_profiles p CROSS JOIN roles r WHERE r.code = 'PROVIDER'
ON CONFLICT DO NOTHING;
