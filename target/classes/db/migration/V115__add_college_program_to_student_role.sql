-- V115__add_college_program_to_student_role.sql
-- Subsystem: Academic Affiliation for STUDENT Role Users & Student Profiles (college_id & program_id)
-- Required for: Institutional Equity Statistics, Degree Audit, Regulatory Compliance, & Advising
-- Standard Compliance: MySQL 8.0 / InnoDB / utf8mb4

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Extend student_profiles with college_id column
ALTER TABLE student_profiles
    ADD COLUMN college_id BIGINT NULL AFTER program_id;

-- 2. Foreign key constraint linking student_profiles.college_id to departments(id)
ALTER TABLE student_profiles
    ADD CONSTRAINT fk_student_profile_college
        FOREIGN KEY (college_id) REFERENCES departments (id) ON DELETE SET NULL;

-- 3. Performance index on student_profiles (college_id)
CREATE INDEX idx_student_profiles_college ON student_profiles (college_id);

-- 4. Backfill student_profiles.college_id from academic program
UPDATE student_profiles sp
INNER JOIN programs p ON sp.program_id = p.id
SET sp.college_id = COALESCE(p.college_id, p.department_id)
WHERE sp.college_id IS NULL;

-- 5. Backfill users.college_id and users.program_id for student users from their profiles
UPDATE users u
INNER JOIN student_profiles sp ON sp.user_id = u.id
INNER JOIN programs p ON sp.program_id = p.id
SET u.program_id = p.id,
    u.college_id = COALESCE(p.college_id, p.department_id)
WHERE u.program_id IS NULL OR u.college_id IS NULL;

SET FOREIGN_KEY_CHECKS = 1;
