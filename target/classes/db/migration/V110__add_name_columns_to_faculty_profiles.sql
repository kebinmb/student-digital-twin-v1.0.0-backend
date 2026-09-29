-- V110__add_name_columns_to_faculty_profiles.sql
-- Subsystem: Faculty Profiling Name Fields (First Name, Middle Name, Last Name, Suffix)
-- Standard Compliance: MySQL 8.0 / InnoDB / utf8mb4

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Extend faculty_profiles with explicit name fields
ALTER TABLE faculty_profiles
    ADD COLUMN first_name VARCHAR(50) NULL AFTER faculty_id_number,
    ADD COLUMN middle_name VARCHAR(50) NULL AFTER first_name,
    ADD COLUMN last_name VARCHAR(50) NULL AFTER middle_name,
    ADD COLUMN suffix VARCHAR(10) NULL AFTER last_name;

-- 2. Populate initial names for seeded test faculty accounts if currently empty
UPDATE faculty_profiles SET first_name = 'Juan', middle_name = 'Ramos', last_name = 'Dela Cruz' WHERE faculty_id_number = 'FAC-2026-001' AND (first_name IS NULL OR first_name = '');
UPDATE faculty_profiles SET first_name = 'Maria', middle_name = 'Santos', last_name = 'Reyes' WHERE faculty_id_number = 'FAC-2026-002' AND (first_name IS NULL OR first_name = '');
UPDATE faculty_profiles SET first_name = 'Roberto', middle_name = 'Gomez', last_name = 'Alcantara' WHERE faculty_id_number = 'FAC-2026-003' AND (first_name IS NULL OR first_name = '');
UPDATE faculty_profiles SET first_name = 'Apolinario', middle_name = 'Perez', last_name = 'Mabini' WHERE faculty_id_number = 'FAC-2026-004' AND (first_name IS NULL OR first_name = '');
