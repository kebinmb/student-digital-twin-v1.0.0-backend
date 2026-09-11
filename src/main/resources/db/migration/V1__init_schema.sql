-- V1__init_schema.sql
-- Subsystem: Unified Baseline Schema Definition (All 36 Tables)
-- Standard Compliance: MySQL 8.0 (InnoDB, utf8mb4, utf8mb4_0900_ai_ci)

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. Authentication & Security Domain
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    college_id BIGINT NULL,
    program_id BIGINT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_college FOREIGN KEY (college_id) REFERENCES departments (id) ON DELETE SET NULL,
    CONSTRAINT fk_users_program FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_users_college ON users (college_id);
CREATE INDEX idx_users_program ON users (program_id);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens (token);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);

CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    username VARCHAR(50) NULL,
    action VARCHAR(50) NOT NULL,
    entity_name VARCHAR(100) NULL,
    entity_id VARCHAR(100) NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(255) NULL,
    details TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT NULL,
    execution_time_ms BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_name);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);

-- -----------------------------------------------------------------------------
-- 2. Institutional Hierarchy & Academic Calendar
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS campuses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    ched_institutional_code VARCHAR(20) NULL,
    name VARCHAR(100) NOT NULL,
    address TEXT NULL,
    region VARCHAR(50) NOT NULL DEFAULT 'REGION VI',
    contact_number VARCHAR(30) NULL,
    email VARCHAR(100) NULL,
    is_main BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campus_id BIGINT NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'COLLEGE',
    parent_department_id BIGINT NULL,
    dean_user_id BIGINT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_campus_dept_code UNIQUE (campus_id, code),
    CONSTRAINT fk_departments_campus FOREIGN KEY (campus_id) REFERENCES campuses (id) ON DELETE CASCADE,
    CONSTRAINT fk_departments_parent FOREIGN KEY (parent_department_id) REFERENCES departments (id) ON DELETE SET NULL,
    CONSTRAINT fk_departments_dean FOREIGN KEY (dean_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

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

CREATE TABLE IF NOT EXISTS academic_years (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS terms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    term_type VARCHAR(20) NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    enrollment_open BOOLEAN NOT NULL DEFAULT FALSE,
    grading_open BOOLEAN NOT NULL DEFAULT FALSE,
    add_drop_open BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    max_hours_per_class DECIMAL(3, 1) NOT NULL DEFAULT 3.0,
    CONSTRAINT uq_ay_term UNIQUE (academic_year_id, term_type),
    CONSTRAINT fk_terms_ay FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS grading_scales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    numeric_grade DECIMAL(3, 2) NULL,
    percentage_min DECIMAL(5, 2) NOT NULL,
    percentage_max DECIMAL(5, 2) NOT NULL,
    transmuted_grade VARCHAR(10) NULL,
    remarks VARCHAR(50) NOT NULL,
    is_passing BOOLEAN NOT NULL DEFAULT TRUE,
    is_non_numeric BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 3. Fees & Financial Templates
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fee_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS fee_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    default_amount DECIMAL(10, 2) NOT NULL,
    is_per_unit BOOLEAN NOT NULL DEFAULT FALSE,
    is_ched_sanctioned BOOLEAN NOT NULL DEFAULT TRUE,
    is_fhe_billable BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_fee_catalog_category FOREIGN KEY (category_id) REFERENCES fee_categories (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS scholarship_discounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    category VARCHAR(30) NOT NULL DEFAULT 'INSTITUTIONAL',
    discount_percentage DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    fixed_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    applies_to_tuition BOOLEAN NOT NULL DEFAULT TRUE,
    applies_to_misc BOOLEAN NOT NULL DEFAULT TRUE,
    funding_source VARCHAR(100) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS payment_term_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    downpayment_pct DECIMAL(5, 2) NOT NULL,
    prelim_pct DECIMAL(5, 2) NOT NULL,
    midterm_pct DECIMAL(5, 2) NOT NULL,
    semifinal_pct DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    final_pct DECIMAL(5, 2) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 4. Curriculum & OBE Framework
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- 5. Physical Facilities & Scheduling
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

-- -----------------------------------------------------------------------------
-- 6. Student Records & Enrollment Management
-- -----------------------------------------------------------------------------
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

CREATE TABLE IF NOT EXISTS student_course_grades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    term_id BIGINT NULL,
    numerical_grade DECIMAL(3, 2) NOT NULL,
    completion_status VARCHAR(20) NOT NULL DEFAULT 'PASSED',
    is_credited BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_scg_student FOREIGN KEY (student_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_scg_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_scg_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_scg_student_course ON student_course_grades (student_id, course_id);

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

-- -----------------------------------------------------------------------------
-- 7. Dynamic Class Records & Grading Engine
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS section_grading_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id BIGINT NOT NULL UNIQUE,
    midterm_weight DECIMAL(5, 2) NOT NULL DEFAULT 50.00,
    final_weight DECIMAL(5, 2) NOT NULL DEFAULT 50.00,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sgc_section FOREIGN KEY (section_id) REFERENCES class_sections (id) ON DELETE CASCADE,
    CONSTRAINT chk_term_weight_total CHECK (midterm_weight + final_weight = 100.00)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS section_grading_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,
    category_name VARCHAR(50) NOT NULL,
    weight_percentage DECIMAL(5, 2) NOT NULL,
    term_period VARCHAR(10) NOT NULL,
    display_order INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_sgcat_config FOREIGN KEY (config_id) REFERENCES section_grading_configs (id) ON DELETE CASCADE,
    CONSTRAINT chk_term_period CHECK (term_period IN ('MIDTERM', 'FINAL'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_sgcat_config ON section_grading_categories (config_id);

CREATE TABLE IF NOT EXISTS class_record_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    item_title VARCHAR(100) NOT NULL,
    max_points DECIMAL(6, 2) NOT NULL,
    sequence_order INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cri_category FOREIGN KEY (category_id) REFERENCES section_grading_categories (id) ON DELETE CASCADE,
    CONSTRAINT chk_max_points CHECK (max_points > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_cri_category ON class_record_items (category_id);

CREATE TABLE IF NOT EXISTS student_assessment_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    score_earned DECIMAL(6, 2) NULL,
    is_excused BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sas_item FOREIGN KEY (item_id) REFERENCES class_record_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_sas_student FOREIGN KEY (student_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT uq_student_item UNIQUE (item_id, student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_sas_item_student ON student_assessment_scores (item_id, student_id);

SET FOREIGN_KEY_CHECKS = 1;
