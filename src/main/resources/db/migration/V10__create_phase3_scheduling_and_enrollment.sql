-- V10__create_phase3_scheduling_and_enrollment.sql
-- Phase 3: Section Management, Class Scheduling, Faculty Workloads, Student Profiles, and Enrollment Management
-- Standard Compliance: CHED CMO No. 25, s. 2015

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. Physical Facilities (Classrooms and Laboratories)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campus_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    building VARCHAR(100) NOT NULL,
    floor INT NOT NULL DEFAULT 1,
    capacity INT NOT NULL DEFAULT 40,
    room_type VARCHAR(30) NOT NULL DEFAULT 'LECTURE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_campus_room_code UNIQUE (campus_id, code),
    CONSTRAINT fk_rooms_campus FOREIGN KEY (campus_id) REFERENCES campuses (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. Class Sections (Subject Offerings per Term)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS class_sections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term_id BIGINT NOT NULL,
    curriculum_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    section_code VARCHAR(30) NOT NULL,
    max_capacity INT NOT NULL DEFAULT 40,
    enrolled_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_term_course_section UNIQUE (term_id, course_id, section_code),
    CONSTRAINT fk_sections_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_sections_curriculum FOREIGN KEY (curriculum_id) REFERENCES curricula (id) ON DELETE RESTRICT,
    CONSTRAINT fk_sections_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. Class Schedules (Timetable Slots per Section)
-- -----------------------------------------------------------------------------
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
    CONSTRAINT fk_schedules_instructor FOREIGN KEY (instructor_user_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_sched_conflict_room (room_id, day_of_week, start_time, end_time),
    INDEX idx_sched_conflict_faculty (instructor_user_id, day_of_week, start_time, end_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. Faculty Workloads (Workload Ledger per Term)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS faculty_workloads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term_id BIGINT NOT NULL,
    faculty_user_id BIGINT NOT NULL,
    regular_units DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    overload_units DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    total_contact_hours DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    is_overload_approved BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by_user_id BIGINT NULL,
    CONSTRAINT uq_term_faculty_workload UNIQUE (term_id, faculty_user_id),
    CONSTRAINT fk_workload_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_workload_faculty FOREIGN KEY (faculty_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_workload_approver FOREIGN KEY (approved_by_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. Student Profiles (Institutional Student Records)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS student_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    student_number VARCHAR(30) NOT NULL UNIQUE,
    program_id BIGINT NOT NULL,
    curriculum_id BIGINT NOT NULL,
    year_level INT NOT NULL DEFAULT 1,
    enrollment_status VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    is_graduating BOOLEAN NOT NULL DEFAULT FALSE,
    total_units_earned DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    cumulative_gpa DECIMAL(3, 2) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_student_program FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_curriculum FOREIGN KEY (curriculum_id) REFERENCES curricula (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6. Student Course Grades (Historical Transcript for Prerequisite Checking)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS student_course_grades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    term_id BIGINT NULL,
    numerical_grade DECIMAL(3, 2) NOT NULL,
    completion_status VARCHAR(20) NOT NULL DEFAULT 'PASSED',
    is_credited BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scg_student FOREIGN KEY (student_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_scg_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_scg_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE SET NULL,
    INDEX idx_scg_student_course (student_id, course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 7. Student Enrollments (Term-Level Enrollment Transactions)
-- -----------------------------------------------------------------------------
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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 8. Enrollment Course Items (Itemized Enlistment per Section)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS enrollment_course_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    final_numerical_grade DECIMAL(3, 2) NULL,
    completion_status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    CONSTRAINT uq_enrollment_section UNIQUE (enrollment_id, section_id),
    CONSTRAINT fk_items_enrollment FOREIGN KEY (enrollment_id) REFERENCES student_enrollments (id) ON DELETE CASCADE,
    CONSTRAINT fk_items_section FOREIGN KEY (section_id) REFERENCES class_sections (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 9. Seed Baseline Data for Scheduling & Advising
-- -----------------------------------------------------------------------------
-- Seed Sample Rooms in Talisay Main Campus (campus_id = 1)
INSERT INTO rooms (campus_id, code, name, building, floor, capacity, room_type, is_active)
VALUES
(1, 'TAL-IT-LAB1', 'Computer Laboratory 1', 'Engineering & Tech Building', 2, 40, 'LABORATORY', TRUE),
(1, 'TAL-IT-LAB2', 'Computer Laboratory 2', 'Engineering & Tech Building', 2, 40, 'LABORATORY', TRUE),
(1, 'TAL-ENG-301', 'Engineering Lecture Hall 301', 'Engineering & Tech Building', 3, 50, 'LECTURE', TRUE),
(1, 'TAL-ENG-302', 'Engineering Lecture Hall 302', 'Engineering & Tech Building', 3, 50, 'LECTURE', TRUE),
(1, 'TAL-GEN-101', 'General Education Hall 101', 'Academic Building', 1, 45, 'LECTURE', TRUE);

-- Seed Sample Student Profile for student_john (user_id = 6)
-- Enrolled in BSIT program and BSIT-2026 active curriculum
INSERT INTO student_profiles (user_id, student_number, program_id, curriculum_id, year_level, enrollment_status, is_graduating, total_units_earned, cumulative_gpa)
VALUES
(6, '2026-IT-0001', 
 (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 
 (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), 
 2, 'REGULAR', FALSE, 26.00, 1.75);

-- Seed Historical Passed Grades for student_john (Enabling Prerequisite Fulfillment)
-- Passed 1st Year Subjects (GE-USELF 1.50, GE-RPH 1.75, GE-PC 1.50, PE-1 1.25, NSTP-1 1.25)
INSERT INTO student_course_grades (student_id, course_id, numerical_grade, completion_status, is_credited)
VALUES
((SELECT id FROM student_profiles WHERE student_number = '2026-IT-0001' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-USELF' LIMIT 1), 1.50, 'PASSED', TRUE),
((SELECT id FROM student_profiles WHERE student_number = '2026-IT-0001' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-RPH' LIMIT 1), 1.75, 'PASSED', TRUE),
((SELECT id FROM student_profiles WHERE student_number = '2026-IT-0001' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-PC' LIMIT 1), 1.50, 'PASSED', TRUE),
((SELECT id FROM student_profiles WHERE student_number = '2026-IT-0001' LIMIT 1), (SELECT id FROM courses WHERE code = 'PE-1' LIMIT 1), 1.25, 'PASSED', TRUE),
((SELECT id FROM student_profiles WHERE student_number = '2026-IT-0001' LIMIT 1), (SELECT id FROM courses WHERE code = 'NSTP-1' LIMIT 1), 1.25, 'PASSED', TRUE);

SET FOREIGN_KEY_CHECKS = 1;