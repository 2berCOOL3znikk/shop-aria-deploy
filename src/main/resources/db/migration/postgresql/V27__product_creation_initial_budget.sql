-- V26 may already be applied to a preview; preserve its checksum and existing owner choices.
ALTER TABLE product_creation_settings ALTER COLUMN daily_budget SET DEFAULT 5;
UPDATE product_creation_settings SET daily_budget=5
 WHERE consent_revision=0 AND text_consent_enabled=false AND budget_used=0
   AND budget_date IS NULL AND last_request_at IS NULL AND daily_budget=10;
