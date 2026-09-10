-- V13__enforce_academic_hierarchy_linkages.sql
-- Enforce academic hierarchy linkages across College, Department, Program, and Accounts
-- Standard Compliance: Strict multi-level scoping for DEAN, CHAIRPERSON, and FACULTY

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Add college_id to programs table
ALTER TABLE programs
    ADD COLUMN college_id BIGINT NULL AFTER department_id,
    ADD CONSTRAINT fk_program_college FOREIGN KEY (college_id) REFERENCES departments (id) ON DELETE RESTRICT;

-- Backfill college_id for existing programs
UPDATE programs p
JOIN departments d ON p.department_id = d.id
SET p.college_id = COALESCE(d.parent_department_id, d.id);

CREATE INDEX idx_programs_college ON programs (college_id);

-- 2. Add college_id and program_id to users table
ALTER TABLE users
    ADD COLUMN college_id BIGINT NULL AFTER enabled,
    ADD COLUMN program_id BIGINT NULL AFTER college_id,
    ADD CONSTRAINT fk_users_college FOREIGN KEY (college_id) REFERENCES departments (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_users_program FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE SET NULL;

CREATE INDEX idx_users_college ON users (college_id);
CREATE INDEX idx_users_program ON users (program_id);

-- 3. Add college_id and program_id to faculty_profiles table
ALTER TABLE faculty_profiles
    ADD COLUMN college_id BIGINT NULL AFTER user_id,
    ADD COLUMN program_id BIGINT NULL AFTER college_id,
    ADD CONSTRAINT fk_faculty_college FOREIGN KEY (college_id) REFERENCES departments (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_faculty_program FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE SET NULL;

CREATE INDEX idx_faculty_college ON faculty_profiles (college_id);
CREATE INDEX idx_faculty_program ON faculty_profiles (program_id);

-- 4. Seed academic assignment chains for demo accounts
-- Dean Morris (user_id = 2): DEAN of College of Computer Studies (CCS, id = 7)
UPDATE users SET college_id = 7 WHERE id = 2;
UPDATE faculty_profiles SET college_id = 7 WHERE user_id = 2;

-- Chair Clark (user_id = 3): CHAIRPERSON of BSIT program under CCS (id = 7)
UPDATE users 
SET college_id = 7, 
    program_id = (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1) 
WHERE id = 3;

UPDATE faculty_profiles 
SET college_id = 7, 
    program_id = (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1) 
WHERE user_id = 3;

-- Faculty Alice (user_id = 4): Dedicated Teaching Faculty under CCS / BSIT
INSERT INTO user_roles (user_id, role) VALUES (4, 'FACULTY') ON DUPLICATE KEY UPDATE role = role;
UPDATE users 
SET college_id = 7, 
    program_id = (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1) 
WHERE id = 4;

INSERT INTO faculty_profiles (user_id, college_id, program_id, faculty_id_number, highest_degree, academic_rank, prc_license_no, employment_status, is_tenured)
VALUES (4, 7, (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'FAC-2026-0004', 'MASTERS', 'INSTRUCTOR_I', 'PRC-0012345', 'FULL_TIME', TRUE)
ON DUPLICATE KEY UPDATE college_id = VALUES(college_id), program_id = VALUES(program_id);

SET FOREIGN_KEY_CHECKS = 1;
