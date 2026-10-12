-- V116__add_production_indexes.sql
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Faculty Attendance
CREATE INDEX idx_facatt_user_verified
    ON faculty_attendance_records (faculty_user_id, verified_at);

-- 2. Attendance Records
CREATE INDEX idx_attrec_student_status
    ON attendance_records (student_profile_id, attendance_status);

-- 3. Campus Notices
CREATE INDEX idx_notices_published_window
    ON campus_notices (is_published, publish_at, expires_at);

-- 4. Student Enrollments
CREATE INDEX idx_enrollment_term_status
    ON student_enrollments (term_id, status);

SET FOREIGN_KEY_CHECKS = 1;