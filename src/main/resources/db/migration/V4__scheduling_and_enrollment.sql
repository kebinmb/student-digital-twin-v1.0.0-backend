-- V4__scheduling_and_enrollment.sql
-- Subsystem: Class Scheduling, Sections, Faculty Workloads & Student Enrollment Operations

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS faculty_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    college_id BIGINT NULL,
    program_id BIGINT NULL,
    faculty_id_number VARCHAR(30) NOT NULL UNIQUE,
    highest_degree VARCHAR(50) NOT NULL DEFAULT 'BACHELORS',
    academic_rank VARCHAR(50) NOT NULL DEFAULT 'INSTRUCTOR_I',
    prc_license_no VARCHAR(50) NULL,
    employment_status VARCHAR(30) NOT NULL DEFAULT 'FULL_TIME',
    is_tenured BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_faculty_profile_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_faculty_college FOREIGN KEY (college_id) REFERENCES departments (id) ON DELETE SET NULL,
    CONSTRAINT fk_faculty_program FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_faculty_college ON faculty_profiles (college_id);
CREATE INDEX idx_faculty_program ON faculty_profiles (program_id);

CREATE TABLE IF NOT EXISTS student_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    student_number VARCHAR(30) NOT NULL UNIQUE,
    program_id BIGINT NOT NULL,
    curriculum_id BIGINT NOT NULL,
    year_level INT NOT NULL DEFAULT 1,
    enrollment_status VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    student_classification VARCHAR(30) NOT NULL DEFAULT 'CONTINUING',
    is_graduating BOOLEAN NOT NULL DEFAULT FALSE,
    total_units_earned DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    cumulative_gpa DECIMAL(3, 2) NULL,
    financial_clearance VARCHAR(20) NOT NULL DEFAULT 'CLEARED',
    departmental_clearance VARCHAR(20) NOT NULL DEFAULT 'CLEARED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_student_program FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_curriculum FOREIGN KEY (curriculum_id) REFERENCES curricula (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS class_sections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term_id BIGINT NOT NULL,
    curriculum_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    section_code VARCHAR(30) NOT NULL,
    max_capacity INT NOT NULL DEFAULT 40,
    enrolled_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    grade_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    primary_instructor_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_term_course_section UNIQUE (term_id, course_id, section_code),
    CONSTRAINT fk_sections_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_sections_curriculum FOREIGN KEY (curriculum_id) REFERENCES curricula (id) ON DELETE RESTRICT,
    CONSTRAINT fk_sections_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_sections_primary_instructor FOREIGN KEY (primary_instructor_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS class_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    instructor_user_id BIGINT NULL,
    day_of_week VARCHAR(15) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    schedule_type VARCHAR(20) NOT NULL DEFAULT 'LECTURE',
    CONSTRAINT fk_schedules_section FOREIGN KEY (section_id) REFERENCES class_sections (id) ON DELETE CASCADE,
    CONSTRAINT fk_schedules_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_schedules_instructor FOREIGN KEY (instructor_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_sched_conflict_room ON class_schedules (room_id, day_of_week, start_time, end_time);
CREATE INDEX idx_sched_conflict_faculty ON class_schedules (instructor_user_id, day_of_week, start_time, end_time);

CREATE TABLE IF NOT EXISTS faculty_workloads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term_id BIGINT NOT NULL,
    faculty_user_id BIGINT NOT NULL,
    regular_units DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    overload_units DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    total_contact_hours DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    is_overload_approved BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by_user_id BIGINT NULL,
    number_of_preparations INT NOT NULL DEFAULT 0,
    custom_max_load_units DECIMAL(5, 2) NULL,
    override_reason VARCHAR(500) NULL,
    overridden_by_user_id BIGINT NULL,
    CONSTRAINT uq_term_faculty_workload UNIQUE (term_id, faculty_user_id),
    CONSTRAINT fk_workload_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_workload_faculty FOREIGN KEY (faculty_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_workload_approver FOREIGN KEY (approved_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_workload_overridden_by FOREIGN KEY (overridden_by_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS student_enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    enrollment_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_credit_units DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    is_overload_approved BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_student_term_enrollment UNIQUE (student_id, term_id),
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS enrollment_course_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    final_numerical_grade DECIMAL(3, 2) NULL,
    completion_status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    CONSTRAINT uq_enrollment_section UNIQUE (enrollment_id, section_id),
    CONSTRAINT fk_items_enrollment FOREIGN KEY (enrollment_id) REFERENCES student_enrollments (id) ON DELETE CASCADE,
    CONSTRAINT fk_items_section FOREIGN KEY (section_id) REFERENCES class_sections (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

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
    CONSTRAINT fk_equiv_approver FOREIGN KEY (approved_by_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_equiv_student ON course_equivalencies (student_id);
CREATE INDEX idx_equiv_course ON course_equivalencies (internal_course_id);

-- Seed Faculty & Student Fixtures
INSERT INTO faculty_profiles (id, user_id, college_id, program_id, faculty_id_number, highest_degree, academic_rank, prc_license_no, employment_status, is_tenured)
VALUES (1, 5, 1, 1, 'FAC-2023-001', 'MASTERS', 'ASSISTANT_PROFESSOR_I', 'PRC-0987654', 'FULL_TIME', TRUE)
AS new_fac ON DUPLICATE KEY UPDATE academic_rank = new_fac.academic_rank;

INSERT INTO student_profiles (id, user_id, student_number, program_id, curriculum_id, year_level, enrollment_status, student_classification, is_graduating, total_units_earned, cumulative_gpa, financial_clearance, departmental_clearance)
VALUES (1, 6, '2023-0001-T', 1, 1, 3, 'REGULAR', 'CONTINUING', FALSE, 72.00, 1.45, 'CLEARED', 'CLEARED')
AS new_stud ON DUPLICATE KEY UPDATE year_level = new_stud.year_level, cumulative_gpa = new_stud.cumulative_gpa;

INSERT INTO class_sections (id, term_id, curriculum_id, course_id, section_code, max_capacity, enrolled_count, status, grade_status, primary_instructor_id)
VALUES (1, 4, 1, 6, 'BSIT-3A', 40, 1, 'OPEN', 'DRAFT', 5)
AS new_sec ON DUPLICATE KEY UPDATE status = new_sec.status;

INSERT INTO class_schedules (id, section_id, room_id, instructor_user_id, day_of_week, start_time, end_time, schedule_type)
VALUES (1, 1, 1, 5, 'MONDAY', '08:00:00', '11:00:00', 'LABORATORY')
AS new_sched ON DUPLICATE KEY UPDATE schedule_type = new_sched.schedule_type;

SET FOREIGN_KEY_CHECKS = 1;
