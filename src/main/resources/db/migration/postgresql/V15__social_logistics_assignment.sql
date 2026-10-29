ALTER TABLE social_proposals ADD COLUMN assignee_id BIGINT REFERENCES app_users(id);
ALTER TABLE social_proposals ADD COLUMN planned_at TIMESTAMP;
ALTER TABLE social_proposals ADD COLUMN logistics_status VARCHAR(20) NOT NULL DEFAULT 'UNPLANNED';
