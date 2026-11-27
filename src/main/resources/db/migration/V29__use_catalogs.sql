-- What a studio package includes is chosen from the service catalog, not typed.
CREATE TABLE studio_offering_services (
    offering_id BIGINT NOT NULL REFERENCES studio_offerings(id) ON DELETE CASCADE,
    service_id BIGINT NOT NULL REFERENCES service_types(id),
    PRIMARY KEY (offering_id, service_id)
);
ALTER TABLE studio_offerings DROP COLUMN includes;

-- A rental item is a product from the equipment catalog (type + brand + model), not a typed title.
-- Items added before this change only had a title and cannot be matched to a product, so they are removed.
DELETE FROM rental_items;
ALTER TABLE rental_items DROP COLUMN title;
ALTER TABLE rental_items ADD COLUMN model_id BIGINT NOT NULL REFERENCES equipment_models(id);
ALTER TABLE rental_items ADD CONSTRAINT uq_rental_items_shop_model UNIQUE (shop_id, model_id);
