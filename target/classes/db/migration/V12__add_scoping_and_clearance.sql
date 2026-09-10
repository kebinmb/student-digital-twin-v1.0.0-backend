-- V12__add_scoping_and_clearance.sql
-- Add departmental scoping (chairperson assignment) and student clearance status fields

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Add clearance fields to student_profiles
ALTER TABLE student_profiles
    ADD COLUMN financial_clearance VARCHAR(20) NOT NULL DEFAULT 'CLEARED' AFTER cumulative_gpa,
    ADD COLUMN departmental_clearance VARCHAR(20) NOT NULL DEFAULT 'CLEARED' AFTER financial_clearance;

-- 2. Add chairperson_user_id to programs
ALTER TABLE programs
    ADD COLUMN chairperson_user_id BIGINT NULL AFTER is_active,
    ADD CONSTRAINT fk_program_chairperson FOREIGN KEY (chairperson_user_id) REFERENCES users (id) ON DELETE SET NULL;

-- 3. Seed initial dean and chairperson assignments for scoping demonstrations
-- Assign dean_morris (id=2) to College of Computer Studies (CCS, id=7)
UPDATE departments SET dean_user_id = 2 WHERE code = 'CCS';

-- Assign chair_clark (id=3) to BSIT program
UPDATE programs SET chairperson_user_id = 3 WHERE code = 'BSIT';

SET FOREIGN_KEY_CHECKS = 1;
