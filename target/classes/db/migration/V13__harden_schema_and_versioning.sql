-- V13__harden_schema_and_versioning.sql
-- 1. Support 64-character SHA-256 hex tokens for password resets
ALTER TABLE password_reset_tokens MODIFY COLUMN token VARCHAR(100) NOT NULL;

-- 2. Optimistic locking on curricula utilizes existing version_number column (V5)

-- 3. Harden column limits in faculty_workloads (previously added in V11)
ALTER TABLE faculty_workloads
    MODIFY COLUMN custom_max_load_units DECIMAL(5,2) NULL,
    MODIFY COLUMN override_reason VARCHAR(500) NULL;

-- 4. Rehash existing sample seed reset tokens to valid SHA-256 digests
UPDATE password_reset_tokens
SET token = SHA2(token, 256)
WHERE LENGTH(token) < 64;