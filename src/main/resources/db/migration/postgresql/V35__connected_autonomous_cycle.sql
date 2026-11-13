-- Disabled until the owner explicitly authorizes this individual store.
ALTER TABLE store_sync_tasks DROP CONSTRAINT store_sync_tasks_operation_check;
ALTER TABLE store_sync_tasks ADD CONSTRAINT store_sync_tasks_operation_check CHECK (operation IN ('CHECK','SYNC','ORDERS','AUTONOMY'));
ALTER TABLE store_pricing_policies ADD COLUMN autonomy_enabled boolean NOT NULL DEFAULT false;
ALTER TABLE store_pricing_policies ADD COLUMN engine_fallback_consent boolean NOT NULL DEFAULT false;
ALTER TABLE store_pricing_policies ADD COLUMN cycle_interval_minutes integer NOT NULL DEFAULT 60;
ALTER TABLE store_pricing_policies ADD CONSTRAINT ck_store_cycle_interval CHECK (cycle_interval_minutes BETWEEN 15 AND 1440);
ALTER TABLE store_sync_runs ADD COLUMN actions_applied integer NOT NULL DEFAULT 0;
ALTER TABLE store_sync_tasks ADD COLUMN cycle_cursor bigint NOT NULL DEFAULT 0;
ALTER TABLE store_pricing_decisions ADD COLUMN autonomy_task_id bigint;
ALTER TABLE store_pricing_decisions ADD COLUMN autonomy_generation bigint;
ALTER TABLE store_pricing_decisions ADD COLUMN autonomy_lease varchar(50);
ALTER TABLE store_price_changes ADD COLUMN reverts_id bigint REFERENCES store_price_changes(id);
CREATE INDEX idx_store_cycle_runs ON store_sync_runs(store_id,operation,completed_at DESC);
