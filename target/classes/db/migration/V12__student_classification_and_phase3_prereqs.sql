-- V12__student_classification_and_phase3_prereqs.sql
-- Phase 3 Prerequisites: Student Classification, Class Section Grade Lifecycle, Course Equivalencies, and Faculty Profiling (CHED Form E-5)

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Extend student_profiles with classification
ALTER TABLE student_profiles 
    ADD COLUMN student_classification VARCHAR(30) NOT NULL DEFAULT 'CONTINUING';

-- 2. Extend class_sections with grade_status and primary_instructor_id
ALTER TABLE class_sections 
    ADD COLUMN grade_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN primary_instructor_id BIGINT NULL;

ALTER TABLE class_sections 
    ADD CONSTRAINT fk_sections_primary_instructor FOREIGN KEY (primary_instructor_id) REFERENCES users (id) ON DELETE SET NULL;

-- 3. Course Equivalencies for Transferee Crediting Engine
CREATE TABLE IF NOT EXISTS course_equivalencies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    external_institution VARCHAR(150) NOT NULL,
    external_course_code VARCHAR(30) NOT NULL,
    external_course_title VARCHAR(150) NOT NULL,
    internal_course_id BIGINT NOT NULL,
    external_numerical_grade DECIMAL(3, 2) NOT NULL,
    credits_granted DECIMAL(4, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    approved_by_user_id BIGINT NULL,
    remarks VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_equiv_student FOREIGN KEY (student_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_equiv_course FOREIGN KEY (internal_course_id) REFERENCES courses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_equiv_approver FOREIGN KEY (approved_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_equiv_student (student_id),
    INDEX idx_equiv_course (internal_course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 4. Faculty Profiles for Credentials & CHED Form E-5 Reporting
CREATE TABLE IF NOT EXISTS faculty_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    faculty_id_number VARCHAR(30) NOT NULL UNIQUE,
    highest_degree VARCHAR(50) NOT NULL DEFAULT 'BACHELORS',
    academic_rank VARCHAR(50) NOT NULL DEFAULT 'INSTRUCTOR_I',
    prc_license_no VARCHAR(50) NULL,
    employment_status VARCHAR(30) NOT NULL DEFAULT 'FULL_TIME',
    is_tenured BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_faculty_profile_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Seed Initial Faculty Profiles for Existing Academic Users
INSERT INTO faculty_profiles (user_id, faculty_id_number, highest_degree, academic_rank, prc_license_no, employment_status, is_tenured)
VALUES
(2, 'FAC-2026-0002', 'DOCTORATE', 'PROFESSOR_I', 'PRC-0098765', 'FULL_TIME', TRUE),
(3, 'FAC-2026-0003', 'MASTERS', 'ASSOCIATE_PROFESSOR_I', 'PRC-0054321', 'FULL_TIME', TRUE);

SET FOREIGN_KEY_CHECKS = 1;
