ALTER TABLE company_registration_requests DROP CONSTRAINT company_request_status;
ALTER TABLE company_registration_requests ADD CONSTRAINT company_request_status CHECK (status IN ('SUBMITTED','IN_REVIEW','VERIFIED','REJECTED','PREPARING','READY','ACTIVE'));
ALTER TABLE company_registration_requests ADD COLUMN workspace_binding varchar(80) UNIQUE;
ALTER TABLE company_registration_requests ADD COLUMN workspace_url varchar(500);
ALTER TABLE company_registration_requests ADD COLUMN setup_secret_cipher text;
ALTER TABLE company_registration_requests ADD COLUMN activation_deadline timestamptz;
ALTER TABLE company_registration_requests ADD COLUMN workspace_checked_at timestamptz;
ALTER TABLE company_registration_requests ADD COLUMN workspace_activated_at timestamptz;

-- Installed explicitly by an operator in a new, empty company database, never by a public request.
CREATE TABLE workspace_activation (
 id bigint PRIMARY KEY CHECK (id=1),
 request_id varchar(36) NOT NULL UNIQUE,
 binding varchar(80) NOT NULL,
 owner_user_id bigint NOT NULL REFERENCES app_users(id),
 owner_email_hash varchar(64) NOT NULL,
 access_hash varchar(64) NOT NULL,
 probe_hash varchar(64) NOT NULL,
 deadline timestamptz NOT NULL,
 claimed_at timestamptz,
 version bigint NOT NULL DEFAULT 0
);
