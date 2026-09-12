-- V3__curriculum_and_obe.sql
-- Subsystem: Curriculum Design, Degree Programs, Course Catalog & OBE Matrix (CILO / PILO)

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS programs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT NOT NULL,
    college_id BIGINT NULL,
    chairperson_user_id BIGINT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    major VARCHAR(100) NULL,
    degree_level VARCHAR(30) NOT NULL DEFAULT 'UNDERGRADUATE',
    ched_cmo_reference VARCHAR(100) NULL,
    government_permit VARCHAR(100) NULL,
    total_units_required INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_programs_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_programs_college FOREIGN KEY (college_id) REFERENCES departments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_programs_chairperson FOREIGN KEY (chairperson_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_programs_college ON programs (college_id);
CREATE INDEX idx_programs_department ON programs (department_id);

ALTER TABLE users 
    ADD CONSTRAINT fk_users_program FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    title VARCHAR(150) NOT NULL,
    lecture_units DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    lab_units DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    credit_units DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    contact_hours_lec INT NOT NULL DEFAULT 0,
    contact_hours_lab INT NOT NULL DEFAULT 0,
    category VARCHAR(30) NOT NULL DEFAULT 'PROFESSIONAL_MAJOR',
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS curricula (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    program_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    version_number INT NOT NULL DEFAULT 1,
    effective_academic_year VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_curricula_program FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS curriculum_courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    curriculum_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    year_level INT NOT NULL,
    sequence_order INT NOT NULL DEFAULT 1,
    category VARCHAR(30) NOT NULL DEFAULT 'PROFESSIONAL_MAJOR',
    semester VARCHAR(20) NOT NULL,
    CONSTRAINT uq_curriculum_course UNIQUE (curriculum_id, course_id),
    CONSTRAINT fk_curr_course_curriculum FOREIGN KEY (curriculum_id) REFERENCES curricula (id) ON DELETE CASCADE,
    CONSTRAINT fk_curr_course_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course_prerequisites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    prerequisite_course_id BIGINT NOT NULL,
    rule_type VARCHAR(20) NOT NULL DEFAULT 'HARD',
    min_grade_required VARCHAR(10) NOT NULL DEFAULT '3.00',
    CONSTRAINT uq_course_prereq UNIQUE (course_id, prerequisite_course_id),
    CONSTRAINT fk_prereq_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_prereq_required FOREIGN KEY (prerequisite_course_id) REFERENCES courses (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS program_outcomes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    program_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL,
    description TEXT NOT NULL,
    CONSTRAINT uq_program_pilo UNIQUE (program_id, code),
    CONSTRAINT fk_pilo_program FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course_outcomes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL,
    description TEXT NOT NULL,
    blooms_level VARCHAR(30) NOT NULL,
    CONSTRAINT uq_course_cilo UNIQUE (course_id, code),
    CONSTRAINT fk_cilo_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS cilo_pilo_mappings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_outcome_id BIGINT NOT NULL,
    program_outcome_id BIGINT NOT NULL,
    mapping_type VARCHAR(10) NOT NULL,
    CONSTRAINT uq_cilo_pilo_map UNIQUE (course_outcome_id, program_outcome_id),
    CONSTRAINT fk_mapping_cilo FOREIGN KEY (course_outcome_id) REFERENCES course_outcomes (id) ON DELETE CASCADE,
    CONSTRAINT fk_mapping_pilo FOREIGN KEY (program_outcome_id) REFERENCES program_outcomes (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Seed Programs, Courses & OBE Matrix
INSERT INTO programs (id, department_id, college_id, chairperson_user_id, code, name, major, degree_level, ched_cmo_reference, government_permit, total_units_required, is_active)
VALUES (1, 5, 1, 4, 'BSIT', 'Bachelor of Science in Information Technology', 'Software Engineering & Analytics', 'UNDERGRADUATE', 'CMO No. 25 s. 2015', 'GR-2018-001', 146, TRUE),
       (2, 6, 2, NULL, 'BSCpE', 'Bachelor of Science in Computer Engineering', 'Embedded Systems', 'UNDERGRADUATE', 'CMO No. 87 s. 2017', 'GR-2018-002', 160, TRUE),
       (3, 4, 4, NULL, 'BSBA', 'Bachelor of Science in Business Administration', 'Financial Management', 'UNDERGRADUATE', 'CMO No. 17 s. 2017', 'GR-2018-003', 138, TRUE),
       (4, 3, 3, NULL, 'BSED', 'Bachelor of Secondary Education', 'Mathematics', 'UNDERGRADUATE', 'CMO No. 75 s. 2017', 'GR-2018-004', 142, TRUE)
AS new_prog ON DUPLICATE KEY UPDATE name = new_prog.name, total_units_required = new_prog.total_units_required;

INSERT INTO courses (id, code, title, lecture_units, lab_units, credit_units, contact_hours_lec, contact_hours_lab, category, description, is_active)
VALUES (1, 'CC101', 'Introduction to Computing', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'Overview of computing, computer architecture, algorithms, and digital logic.', TRUE),
       (2, 'CC102', 'Fundamentals of Programming', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'Basic programming concepts using C++ / Java constructs.', TRUE),
       (3, 'CC103', 'Intermediate Programming', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'Object-oriented programming concepts, inheritance, polymorphism, and abstraction.', TRUE),
       (4, 'IT201', 'Data Structures and Algorithms', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Linear and non-linear data structures, searching, sorting, and algorithmic complexity.', TRUE),
       (5, 'IT202', 'Information Management & Databases', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Relational database management system principles, SQL queries, normalization, and indexing.', TRUE),
       (6, 'IT301', 'Web Systems & Technologies', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Full-stack web application development using modern frameworks, REST APIs, and client-side reactive components.', TRUE),
       (7, 'MATH101', 'Calculus 1', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Limits, continuity, differentiation, and integral calculus application.', TRUE)
AS new_course ON DUPLICATE KEY UPDATE title = new_course.title, credit_units = new_course.credit_units;

INSERT INTO curricula (id, program_id, code, name, status, version_number, effective_academic_year, is_active)
VALUES (1, 1, 'BSIT-2023-V1', 'BSIT CMO 25 s. 2015 Revised Curriculum', 'ACTIVE', 1, 'AY-2023-2024', TRUE),
       (2, 2, 'BSCpE-2023-V1', 'BSCpE CMO 87 s. 2017 Outcome-Based Curriculum', 'ACTIVE', 1, 'AY-2023-2024', TRUE)
AS new_curr ON DUPLICATE KEY UPDATE name = new_curr.name, status = new_curr.status;

INSERT INTO curriculum_courses (id, curriculum_id, course_id, year_level, sequence_order, category, semester)
VALUES (1, 1, 1, 1, 1, 'INSTITUTIONAL_CORE', 'FIRST_SEMESTER'),
       (2, 1, 2, 1, 2, 'INSTITUTIONAL_CORE', 'FIRST_SEMESTER'),
       (3, 1, 3, 1, 1, 'INSTITUTIONAL_CORE', 'SECOND_SEMESTER'),
       (4, 1, 4, 2, 1, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
       (5, 1, 5, 2, 2, 'PROFESSIONAL_MAJOR', 'SECOND_SEMESTER'),
       (6, 1, 6, 3, 1, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER')
AS new_curr_course ON DUPLICATE KEY UPDATE year_level = new_curr_course.year_level, semester = new_curr_course.semester;

INSERT INTO course_prerequisites (id, course_id, prerequisite_course_id, rule_type, min_grade_required)
VALUES (1, 3, 2, 'HARD', '3.00'),
       (2, 4, 3, 'HARD', '3.00'),
       (3, 5, 4, 'HARD', '3.00'),
       (4, 6, 5, 'HARD', '3.00')
AS new_prereq ON DUPLICATE KEY UPDATE rule_type = new_prereq.rule_type, min_grade_required = new_prereq.min_grade_required;

INSERT INTO program_outcomes (id, program_id, code, description)
VALUES (1, 1, 'PILO-01', 'Apply knowledge of computing fundamentals, mathematics, and domain specialization to complex IT problems.'),
       (2, 1, 'PILO-02', 'Design, implement, and evaluate computer-based systems, processes, components, or programs to meet desired needs.'),
       (3, 1, 'PILO-03', 'Analyze local and global impact of computing on individuals, organizations, and society with professional ethics.')
AS new_pilo ON DUPLICATE KEY UPDATE description = new_pilo.description;

INSERT INTO course_outcomes (id, course_id, code, description, blooms_level)
VALUES (1, 6, 'CILO-601', 'Architect secure RESTful web APIs integrated with relational database systems.', 'CREATE'),
       (2, 6, 'CILO-602', 'Develop reactive user interface components enforcing state management and OnPush change detection.', 'APPLY'),
       (3, 6, 'CILO-603', 'Evaluate full-stack web application performance and optimize asset bundle delivery.', 'EVALUATE')
AS new_cilo ON DUPLICATE KEY UPDATE description = new_cilo.description, blooms_level = new_cilo.blooms_level;

INSERT INTO cilo_pilo_mappings (id, course_outcome_id, program_outcome_id, mapping_type)
VALUES (1, 1, 1, 'HIGH'),
       (2, 2, 2, 'HIGH'),
       (3, 3, 3, 'MEDIUM')
AS new_map ON DUPLICATE KEY UPDATE mapping_type = new_map.mapping_type;

-- Link bootstrap users to BSIT program
UPDATE users SET program_id = 1 WHERE id IN (3, 4, 5, 6, 7, 8);

SET FOREIGN_KEY_CHECKS = 1;
