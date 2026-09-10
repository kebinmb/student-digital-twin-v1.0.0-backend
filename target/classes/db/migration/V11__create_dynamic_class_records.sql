-- Flyway Migration Script: V11__create_dynamic_class_records.sql
-- Subsystem: Dynamic Class Record, Assessment Weighting & Score Matrix Engine

-- 1. Section Grading Configuration Header
CREATE TABLE IF NOT EXISTS section_grading_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id BIGINT NOT NULL UNIQUE,
    midterm_weight DECIMAL(5,2) NOT NULL DEFAULT 50.00,
    final_weight DECIMAL(5,2) NOT NULL DEFAULT 50.00,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sgc_section FOREIGN KEY (section_id) REFERENCES class_sections(id) ON DELETE CASCADE,
    CONSTRAINT chk_term_weight_total CHECK (midterm_weight + final_weight = 100.00)
);

-- 2. Section Grading Categories
CREATE TABLE IF NOT EXISTS section_grading_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,
    category_name VARCHAR(50) NOT NULL,
    weight_percentage DECIMAL(5,2) NOT NULL,
    term_period VARCHAR(10) NOT NULL,
    display_order INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_sgcat_config FOREIGN KEY (config_id) REFERENCES section_grading_configs(id) ON DELETE CASCADE,
    CONSTRAINT chk_term_period CHECK (term_period IN ('MIDTERM', 'FINAL'))
);

-- 3. Class Record Assessment Items
CREATE TABLE IF NOT EXISTS class_record_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    item_title VARCHAR(100) NOT NULL,
    max_points DECIMAL(6,2) NOT NULL,
    sequence_order INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cri_category FOREIGN KEY (category_id) REFERENCES section_grading_categories(id) ON DELETE CASCADE,
    CONSTRAINT chk_max_points CHECK (max_points > 0)
);

-- 4. Student Assessment Scores Matrix
CREATE TABLE IF NOT EXISTS student_assessment_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    score_earned DECIMAL(6,2) NULL,
    is_excused BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sas_item FOREIGN KEY (item_id) REFERENCES class_record_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_sas_student FOREIGN KEY (student_id) REFERENCES student_profiles(id) ON DELETE CASCADE,
    CONSTRAINT uq_student_item UNIQUE (item_id, student_id)
);

-- Indexes for query optimization
CREATE INDEX idx_sgcat_config ON section_grading_categories(config_id);
CREATE INDEX idx_cri_category ON class_record_items(category_id);
CREATE INDEX idx_sas_item_student ON student_assessment_scores(item_id, student_id);
