ALTER TABLE connected_stores ADD COLUMN catalog_currency varchar(3);
ALTER TABLE connected_stores ADD COLUMN currency_confirmed_at timestamp;
-- Existing snapshots keep NULL currency until a complete verified import; no currency is guessed.
