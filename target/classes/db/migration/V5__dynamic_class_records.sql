-- V5__dynamic_class_records.sql
-- Subsystem: Dynamic Class Records, Assessment Scores, Grading Schemes & Grade Change Approvals

SET FOREIGN_KEY_CHECKS = 0;

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

CREATE TABLE IF NOT EXISTS grade_change_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    previous_grade DECIMAL(3, 2) NOT NULL,
    new_grade DECIMAL(3, 2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_by_id BIGINT NOT NULL,
    approved_by_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_gcr_student FOREIGN KEY (student_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_gcr_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_gcr_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_gcr_requested_by FOREIGN KEY (requested_by_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_gcr_approved_by FOREIGN KEY (approved_by_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Seed Standard CHMSU Grading Scale
INSERT INTO grading_scales (id, code, numeric_grade, percentage_min, percentage_max, transmuted_grade, remarks, is_passing, is_non_numeric)
VALUES (1, '1.00', 1.00, 97.00, 100.00, '1.00', 'EXCELLENT', TRUE, FALSE),
       (2, '1.25', 1.25, 94.00, 96.99, '1.25', 'SUPERIOR', TRUE, FALSE),
       (3, '1.50', 1.50, 91.00, 93.99, '1.50', 'VERY GOOD', TRUE, FALSE),
       (4, '1.75', 1.75, 88.00, 90.99, '1.75', 'GOOD', TRUE, FALSE),
       (5, '2.00', 2.00, 85.00, 87.99, '2.00', 'VERY SATISFACTORY', TRUE, FALSE),
       (6, '2.25', 2.25, 82.00, 84.99, '2.25', 'SATISFACTORY', TRUE, FALSE),
       (7, '2.50', 2.50, 79.00, 81.99, '2.50', 'FAIR', TRUE, FALSE),
       (8, '2.75', 2.75, 76.00, 78.99, '2.75', 'PASSING', TRUE, FALSE),
       (9, '3.00', 3.00, 75.00, 75.99, '3.00', 'PASSED BARELY', TRUE, FALSE),
       (10, '5.00', 5.00, 0.00, 74.99, '5.00', 'FAILED', FALSE, FALSE),
       (11, 'INC', NULL, 0.00, 0.00, 'INC', 'INCOMPLETE', FALSE, TRUE),
       (12, 'DRP', NULL, 0.00, 0.00, 'DRP', 'DROPPED', FALSE, TRUE)
AS new_gs ON DUPLICATE KEY UPDATE numeric_grade = new_gs.numeric_grade,
                        percentage_min = new_gs.percentage_min,
                        percentage_max = new_gs.percentage_max,
                        transmuted_grade = new_gs.transmuted_grade,
                        remarks = new_gs.remarks,
                        is_passing = new_gs.is_passing,
                        is_non_numeric = new_gs.is_non_numeric;

SET FOREIGN_KEY_CHECKS = 1;
