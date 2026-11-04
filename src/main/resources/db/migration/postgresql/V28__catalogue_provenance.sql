-- Existing business records retain their values; provenance is opt-in per product.
ALTER TABLE product_details ADD COLUMN catalogue_reference varchar(120);
ALTER TABLE product_details ADD COLUMN demonstration boolean NOT NULL DEFAULT false;
