-- Move the single legacy user_type value into an additive role table.
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL CHECK (role IN ('CUSTOMER', 'PROVIDER', 'ADMIN')),
    PRIMARY KEY (user_id, role)
);

INSERT INTO user_roles (user_id, role)
SELECT id,
       CASE user_type
           WHEN 'PROFESSIONAL' THEN 'PROVIDER'
           WHEN 'BOTH' THEN 'CUSTOMER'
           WHEN 'ADMIN' THEN 'ADMIN'
           ELSE 'CUSTOMER'
       END
FROM users;

INSERT INTO user_roles (user_id, role)
SELECT id, 'PROVIDER'
FROM users
WHERE user_type = 'BOTH';

CREATE INDEX idx_user_roles_role ON user_roles(role);
ALTER TABLE users DROP COLUMN user_type;
