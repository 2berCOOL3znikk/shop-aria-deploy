-- The gateway stores only allocation metadata. Business tables stay in independent databases/processes.
CREATE TABLE saas_pool_slots (
 id varchar(36) PRIMARY KEY,
 binding varchar(80) NOT NULL UNIQUE,
 origin varchar(300) NOT NULL UNIQUE,
 secret_cipher text NOT NULL,
 admission_hash varchar(64) NOT NULL UNIQUE,
 state varchar(20) NOT NULL CHECK(state IN ('AVAILABLE','RESERVED','CLAIMED','RETIRED')),
 registration_id varchar(36) UNIQUE,
 created_at timestamp NOT NULL,
 version bigint NOT NULL DEFAULT 0
);
CREATE TABLE saas_cloud_registrations (
 id varchar(36) PRIMARY KEY,
 access_hash varchar(64) NOT NULL UNIQUE,
 email varchar(255) NOT NULL,
 email_hash varchar(64) NOT NULL UNIQUE,
 company_name varchar(200) NOT NULL,
 credential_cipher text,
 fingerprint varchar(64) NOT NULL,
 status varchar(30) NOT NULL CHECK(status IN ('WAITING_CAPACITY','PREPARING','RETRYABLE','ACTIVE')),
 slot_id varchar(36) UNIQUE REFERENCES saas_pool_slots(id),
 processing_consent boolean NOT NULL,
 authority_confirmed boolean NOT NULL,
 consent_version varchar(40) NOT NULL,
 created_at timestamp NOT NULL,
 updated_at timestamp NOT NULL,
 lease_until timestamp,
 attempts integer NOT NULL DEFAULT 0,
 failure_code varchar(40),
 version bigint NOT NULL DEFAULT 0
);
CREATE INDEX saas_registration_recovery ON saas_cloud_registrations(status,lease_until);
CREATE TABLE saas_slot_installation (
 id integer PRIMARY KEY CHECK(id=1),
 binding varchar(80) NOT NULL,
 registration_id varchar(36),
 email_hash varchar(64),
 company_hash varchar(64),
 credential_hash varchar(64),
 owner_user_id bigint REFERENCES app_users(id),
 installed_at timestamp,
 version bigint NOT NULL DEFAULT 0
);
-- Only a new empty database with the dedicated role can be made into a slot.
-- Existing installations, including an empty but already assigned company, are never relabelled.
DO $$
DECLARE requested text := current_setting('aria.workspace_slot_binding',true);
BEGIN
 IF requested IS NOT NULL AND requested<>'' THEN
  IF requested !~ '^business-[a-f0-9]{32}$'
     OR current_database()<>'aria_slot_'||substring(requested from 10)
     OR current_user<>'aria_slot_owner_'||substring(requested from 10)
     OR NOT EXISTS(SELECT 1 FROM pg_roles r JOIN pg_database d ON d.datdba=r.oid
                   WHERE r.rolname=current_user AND d.datname=current_database()
                     AND NOT r.rolsuper AND NOT r.rolcreatedb AND NOT r.rolcreaterole)
     OR EXISTS(SELECT 1 FROM pg_auth_members m JOIN pg_roles r ON r.oid=m.member WHERE r.rolname=current_user)
     OR (SELECT count(*) FROM saas_organization)<>1
     OR NOT EXISTS(SELECT 1 FROM saas_organization WHERE id=1 AND organization_key='demo' AND demonstration=true)
     OR EXISTS(SELECT 1 FROM app_users) OR EXISTS(SELECT 1 FROM products)
     OR EXISTS(SELECT 1 FROM orders) OR EXISTS(SELECT 1 FROM connected_stores)
     OR EXISTS(SELECT 1 FROM company_registration_requests) OR EXISTS(SELECT 1 FROM audit_events)
  THEN RAISE EXCEPTION 'Workspace slot requires a new empty database and an isolated unprivileged owner'; END IF;
  UPDATE saas_organization SET organization_key=requested,name='ARIA',demonstration=false,impact_enabled=false WHERE id=1;
  INSERT INTO saas_slot_installation(id,binding) VALUES(1,requested);
 END IF;
END $$;
