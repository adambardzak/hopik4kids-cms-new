-- V2: store JSON-ish fields as text instead of jsonb.
-- Hibernate binds String params as varchar, which Postgres refuses to cast into jsonb implicitly.
-- We do not run jsonb queries on these columns, so text is sufficient.

ALTER TABLE audit_log ALTER COLUMN meta TYPE text USING meta::text;
ALTER TABLE media ALTER COLUMN variants TYPE text USING variants::text;
