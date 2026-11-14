-- Skills had no order. A number the database fills in on every insert records the order they were added in;
-- rows that already exist get numbers in no particular order.
ALTER TABLE professional_profile_skills ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY;
CREATE INDEX idx_professional_profile_skills_order ON professional_profile_skills (profile_id, id);
