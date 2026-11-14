-- Some services (like "other services") let the member add a name of their own when adding a price.
-- Turn it on for another service with: UPDATE service_types SET asks_name = TRUE WHERE name = '...';
ALTER TABLE service_types ADD COLUMN asks_name BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE service_types SET asks_name = TRUE WHERE name = 'سایر خدمات';
