ALTER TABLE app_users ADD COLUMN IF NOT EXISTS token_version BIGINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX IF NOT EXISTS idx_app_users_email_lower ON app_users (lower(email));
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipped_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_orders_shipped_at ON orders (shipped_at);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_name VARCHAR(500);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_sku VARCHAR(64);
-- Legacy order names and shipment dates are not reconstructed as historical facts.
