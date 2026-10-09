-- V116__add_production_indexes.sql
-- Subsystem: Production Readiness Index Optimization & Performance Tuning
-- Fixed: Added IF NOT EXISTS to all CREATE INDEX statements to prevent
--        partial migration failure on retry or if index already exists.

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Accelerate Faculty Attendance lookups by faculty member and verification timestamp
CREATE INDEX IF NOT EXISTS idx_facatt_user_verified
    ON faculty_attendance_records (faculty_user_id, verified_at);

-- 2. Accelerate Attendance Record queries by student and attendance status
--    for ML/telemetry risk scoring
CREATE INDEX IF NOT EXISTS idx_attrec_student_status
    ON attendance_records (student_profile_id, status);

-- 3. Accelerate active campus notices filtering by published state,
--    publish date, and expiration window
CREATE INDEX IF NOT EXISTS idx_notices_published_window
    ON campus_notices (is_published, publish_at, expires_at);

-- 4. Accelerate student enrollment lookups by term and enrollment status
CREATE INDEX IF NOT EXISTS idx_enrollment_term_status
    ON student_enrollments (term_id, status);

SET FOREIGN_KEY_CHECKS = 1;
