ALTER TABLE inventory_lots ADD COLUMN storage_requirements VARCHAR(700);
ALTER TABLE inventory_lots ADD COLUMN handling_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE inventory_lots ADD COLUMN handling_revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE inventory_lots ADD COLUMN handling_checked_at TIMESTAMP;
ALTER TABLE inventory_lots ADD COLUMN handling_checked_by VARCHAR(255);
