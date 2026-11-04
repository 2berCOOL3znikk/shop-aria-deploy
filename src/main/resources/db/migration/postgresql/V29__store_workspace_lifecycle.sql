-- Explicit owner classification. Existing connections are preserved and never guessed to be tests.
ALTER TABLE connected_stores ADD COLUMN archived boolean NOT NULL DEFAULT false;
ALTER TABLE connected_stores ADD COLUMN archived_at timestamp;
ALTER TABLE connected_stores ADD COLUMN test_connection boolean NOT NULL DEFAULT false;
