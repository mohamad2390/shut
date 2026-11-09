-- Days a provider has marked as free or busy. Days with no row are "not set".
CREATE TABLE calendar_days (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day_date DATE NOT NULL,
    status VARCHAR(10) NOT NULL,
    UNIQUE (user_id, day_date)
);
