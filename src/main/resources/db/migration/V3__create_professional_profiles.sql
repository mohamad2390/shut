CREATE TABLE professional_profiles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(150),
    headline VARCHAR(180),
    bio TEXT,
    years_experience INTEGER NOT NULL DEFAULT 0 CHECK (years_experience BETWEEN 0 AND 80),
    base_city VARCHAR(100),
    base_country VARCHAR(100),
    website_url VARCHAR(500),
    instagram_username VARCHAR(100),
    equipment_description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE professional_profile_skills (
    profile_id BIGINT NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    skill VARCHAR(100) NOT NULL,
    PRIMARY KEY (profile_id, skill)
);

CREATE TABLE professional_profile_service_cities (
    profile_id BIGINT NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    city VARCHAR(100) NOT NULL,
    PRIMARY KEY (profile_id, city)
);

CREATE TABLE professional_profile_collaborators (
    profile_id BIGINT NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    collaborator VARCHAR(150) NOT NULL,
    PRIMARY KEY (profile_id, collaborator)
);

CREATE INDEX idx_professional_profiles_base_city ON professional_profiles(base_city);
CREATE INDEX idx_professional_profile_skills_skill ON professional_profile_skills(skill);
CREATE INDEX idx_professional_profile_service_cities_city ON professional_profile_service_cities(city);
