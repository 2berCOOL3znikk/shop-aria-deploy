-- The registration desk uses its own database/process. This table is not a tenant directory.
CREATE TABLE company_registration_requests (
 id varchar(36) PRIMARY KEY,
 company_name varchar(200) NOT NULL,
 owner_name varchar(120) NOT NULL,
 owner_email varchar(255) NOT NULL,
 access_hash varchar(64) NOT NULL UNIQUE,
 status varchar(30) NOT NULL,
 consent_version varchar(40) NOT NULL,
 processing_consent boolean NOT NULL,
 contact_consent boolean NOT NULL,
 created_at timestamp NOT NULL,
 updated_at timestamp NOT NULL,
 reviewed_at timestamp,
 reviewed_by varchar(255),
 review_note varchar(700),
 version bigint NOT NULL DEFAULT 0,
 CONSTRAINT company_request_status CHECK (status IN ('SUBMITTED','IN_REVIEW','VERIFIED','REJECTED'))
);
CREATE INDEX company_request_created ON company_registration_requests(created_at DESC);
