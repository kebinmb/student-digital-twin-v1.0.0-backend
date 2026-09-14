-- V13__add_enrolled_status_to_admissions.sql
-- Add enrollment transition tracking fields to admission applications and student profiles

ALTER TABLE admission_applications
    ADD COLUMN is_enrolled BOOLEAN NOT NULL DEFAULT FALSE AFTER application_status,
    ADD COLUMN enrolled_at TIMESTAMP NULL AFTER is_enrolled,
    ADD COLUMN student_profile_id BIGINT NULL AFTER enrolled_at;

ALTER TABLE student_profiles
    ADD COLUMN admission_application_id BIGINT NULL AFTER user_id;

CREATE INDEX idx_adm_app_status_enrolled ON admission_applications (application_status, is_enrolled);
CREATE INDEX idx_student_profile_adm_app ON student_profiles (admission_application_id);

-- Backfill legacy converted admission applications if any exist
UPDATE admission_applications a
JOIN student_profiles sp ON (
    sp.student_number = a.application_number 
    OR (a.email IS NOT NULL AND LOWER(a.email) = (SELECT LOWER(u.email) FROM users u WHERE u.id = sp.user_id))
)
SET a.application_status = 'ENROLLED',
    a.is_enrolled = TRUE,
    a.student_profile_id = sp.id,
    sp.admission_application_id = a.id
WHERE a.application_status != 'ENROLLED';
