-- Existing owner facts, if any, require confirmation instead of inferred currency/base price.
ALTER TABLE store_product_policies ADD COLUMN currency VARCHAR(3);
ALTER TABLE store_product_policies ADD COLUMN reference_price NUMERIC(19,2);
