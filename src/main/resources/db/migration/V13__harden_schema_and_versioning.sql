-- V13__harden_schema_and_versioning.sql
-- 1. Support 64-character SHA-256 hex tokens for password resets
ALTER TABLE password_reset_tokens MODIFY COLUMN token VARCHAR(100) NOT NULL;

-- 2. Add optimistic locking column to curricula
ALTER TABLE curricula ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 3. Fix missing columns in faculty_workloads (Audit P0-3)
ALTER TABLE faculty_workloads
    ADD COLUMN IF NOT EXISTS number_of_preparations INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS custom_max_load_units DECIMAL(5,2) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS override_reason VARCHAR(500) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS overridden_by_user_id BIGINT DEFAULT NULL;

-- 4. Rehash existing sample seed reset tokens to valid SHA-256 digests
UPDATE password_reset_tokens
SET token = SHA2(token, 256)
WHERE LENGTH(token) < 64;