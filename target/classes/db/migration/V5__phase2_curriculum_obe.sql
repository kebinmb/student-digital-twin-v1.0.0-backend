-- V4_3__phase2_curriculum_obe.sql
-- Phase 2: Curriculum Management, Course Catalogs, and OBE Framework

SET
FOREIGN_KEY_CHECKS = 0;

-- 1. Academic Programs (Degree Offerings linked to Departments & CHED CMOs)
CREATE TABLE programs
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id        BIGINT       NOT NULL,
    code                 VARCHAR(20)  NOT NULL UNIQUE,                  -- e.g., 'BSIT', 'BSCE', 'BSED-ENG'
    name                 VARCHAR(150) NOT NULL,                         -- e.g., 'Bachelor of Science in Information Technology'
    major                VARCHAR(100) NULL,                             -- e.g., 'Network Security' (optional)
    degree_level         VARCHAR(30)  NOT NULL DEFAULT 'UNDERGRADUATE', -- UNDERGRADUATE, GRADUATE, DIPLOMA
    ched_cmo_reference   VARCHAR(100) NULL,                             -- e.g., 'CMO No. 25, Series of 2017'
    government_permit    VARCHAR(100) NULL,                             -- e.g., 'GP No. 123, s. 2018'
    total_units_required INT          NOT NULL,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_program_department FOREIGN KEY (department_id) REFERENCES departments (id)
);

-- 2. Course Catalogs (Master Subject Registry)
CREATE TABLE courses
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    code              VARCHAR(30)   NOT NULL UNIQUE, -- e.g., 'IT 101', 'GE 1'
    title             VARCHAR(150)  NOT NULL,        -- e.g., 'Introduction to Computing'
    lecture_units     DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    lab_units         DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    credit_units      DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    contact_hours_lec INT           NOT NULL DEFAULT 0,
    contact_hours_lab INT           NOT NULL DEFAULT 0,
    description       TEXT NULL,
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE
);

-- 3. Curricula (Curriculum Versioning per Program)
CREATE TABLE curricula
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    program_id              BIGINT       NOT NULL,
    code                    VARCHAR(30)  NOT NULL UNIQUE, -- e.g., 'BSIT-2026'
    name                    VARCHAR(150) NOT NULL,        -- e.g., 'BSIT Curriculum 2026-2030'
    status                  VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    version_number          INT          NOT NULL DEFAULT 1,
    effective_academic_year VARCHAR(20)  NOT NULL,        -- e.g., '2026-2027'
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_curriculum_program FOREIGN KEY (program_id) REFERENCES programs (id)
);

-- 4. Curriculum Courses (Mapping courses to specific Year / Semester blocks in a curriculum)
CREATE TABLE curriculum_courses
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    curriculum_id  BIGINT      NOT NULL,
    course_id      BIGINT      NOT NULL,
    year_level     INT         NOT NULL, -- 1, 2, 3, 4, 5
    sequence_order INT         NOT NULL DEFAULT 1,
    category       VARCHAR(30) NOT NULL DEFAULT 'PROFESSIONAL_MAJOR',
    semester       VARCHAR(20) NOT NULL, -- '1ST_SEM', '2ND_SEM', 'SUMMER'
    CONSTRAINT fk_curr_course_curriculum FOREIGN KEY (curriculum_id) REFERENCES curricula (id),
    CONSTRAINT fk_curr_course_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT uq_curriculum_course UNIQUE (curriculum_id, course_id)
);

-- 5. Course Prerequisite Rules (DAG Structure)
CREATE TABLE course_prerequisites
(
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id              BIGINT      NOT NULL,                -- The target course needing prerequisites
    prerequisite_course_id BIGINT      NOT NULL,                -- The required prerequisite course
    rule_type              VARCHAR(20) NOT NULL DEFAULT 'HARD', -- 'HARD', 'CO_REQUISITE', 'STANDING'
    min_grade_required     VARCHAR(10) NOT NULL DEFAULT '3.00',
    CONSTRAINT fk_prereq_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_prereq_required FOREIGN KEY (prerequisite_course_id) REFERENCES courses (id),
    CONSTRAINT uq_course_prereq UNIQUE (course_id, prerequisite_course_id)
);

-- 6. Outcome-Based Education (OBE) Framework
-- PILOs: Program Intended Learning Outcomes (CHED CMO Competencies)
CREATE TABLE program_outcomes
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    program_id  BIGINT      NOT NULL,
    code        VARCHAR(30) NOT NULL, -- e.g., 'PILO-a', 'PILO-b'
    description TEXT        NOT NULL,
    CONSTRAINT fk_pilo_program FOREIGN KEY (program_id) REFERENCES programs (id),
    CONSTRAINT uq_program_pilo UNIQUE (program_id, code)
);

-- CILOs: Course Intended Learning Outcomes mapped with Bloom's Taxonomy
CREATE TABLE course_outcomes
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id    BIGINT      NOT NULL,
    code         VARCHAR(30) NOT NULL, -- e.g., 'CILO-1'
    description  TEXT        NOT NULL,
    blooms_level VARCHAR(30) NOT NULL, -- 'REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE', 'EVALUATE', 'CREATE'
    CONSTRAINT fk_cilo_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT uq_course_cilo UNIQUE (course_id, code)
);

-- CILO to PILO Alignment Mapping Matrix (I = Introduced, E = Enabled, D = Demonstrated)
CREATE TABLE cilo_pilo_mappings
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_outcome_id  BIGINT      NOT NULL,
    program_outcome_id BIGINT      NOT NULL,
    mapping_type       VARCHAR(10) NOT NULL, -- 'I', 'E', 'D'
    CONSTRAINT fk_mapping_cilo FOREIGN KEY (course_outcome_id) REFERENCES course_outcomes (id),
    CONSTRAINT fk_mapping_pilo FOREIGN KEY (program_outcome_id) REFERENCES program_outcomes (id),
    CONSTRAINT uq_cilo_pilo_map UNIQUE (course_outcome_id, program_outcome_id)
);

SET
FOREIGN_KEY_CHECKS = 1;