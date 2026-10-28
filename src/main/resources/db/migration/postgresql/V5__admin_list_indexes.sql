CREATE INDEX IF NOT EXISTS idx_orders_created_id ON orders (created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_audit_created_id ON audit_events (created_at DESC, id DESC);
