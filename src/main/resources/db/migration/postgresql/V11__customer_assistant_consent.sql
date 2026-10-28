alter table smart_pricing_settings add column public_assistant_ai_enabled boolean not null default false;
alter table smart_pricing_settings add column public_ai_daily_budget integer not null default 10;
alter table smart_pricing_settings add column public_ai_budget_used integer not null default 0;
alter table smart_pricing_settings add column public_ai_budget_date date;
alter table smart_pricing_settings add column public_ai_last_request_at timestamp;
alter table smart_pricing_settings add column consent_revision bigint not null default 0;
alter table pricing_decisions add column ai_last_skipped_at timestamp;
alter table pricing_decisions add column ai_last_skipped_note varchar(1000);
