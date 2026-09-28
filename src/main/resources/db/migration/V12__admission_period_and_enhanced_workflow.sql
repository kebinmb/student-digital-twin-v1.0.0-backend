-- V12__admission_period_and_enhanced_workflow.sql
-- Subsystem: Admission Period Controls, Daily/Total Slot Allocation, CHED Personal Data Profiling & Exam/Interview Lifecycle Workflow

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Admission Period & Operational Slot Configuration Table
CREATE TABLE IF NOT EXISTS admission_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term_id BIGINT NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    daily_slot_limit INT NOT NULL DEFAULT 1000,
    total_opened_slots INT NOT NULL DEFAULT 20000,
    days_open INT NOT NULL DEFAULT 20,
    start_date DATE NULL,
    end_date DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_adm_cfg_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 2. Extend admission_applications table with full CHED CMO / Enhanced Admissions Workflow columns
ALTER TABLE admission_applications
    ADD COLUMN gender_identity VARCHAR(30) NULL AFTER gender,
    ADD COLUMN birth_place VARCHAR(150) NULL AFTER birth_date,
    ADD COLUMN perm_region VARCHAR(100) NULL AFTER zip_code,
    ADD COLUMN perm_province VARCHAR(100) NULL AFTER perm_region,
    ADD COLUMN perm_city_municipality VARCHAR(100) NULL AFTER perm_province,
    ADD COLUMN perm_barangay VARCHAR(100) NULL AFTER perm_city_municipality,
    ADD COLUMN perm_zip_code VARCHAR(10) NULL AFTER perm_barangay,
    ADD COLUMN perm_street_address VARCHAR(255) NULL AFTER perm_zip_code,
    ADD COLUMN deped_school_id VARCHAR(30) NULL AFTER high_school_name,
    ADD COLUMN shs_year_graduated INT NULL AFTER high_school_gwa,
    ADD COLUMN is_underprivileged_homeless BOOLEAN NOT NULL DEFAULT FALSE AFTER is_first_generation_college,
    ADD COLUMN scholarship_grant_type VARCHAR(60) NULL AFTER is_underprivileged_homeless,
    ADD COLUMN exam_score DECIMAL(5, 2) NULL AFTER application_status,
    ADD COLUMN exam_remarks VARCHAR(255) NULL AFTER exam_score,
    ADD COLUMN interview_score DECIMAL(5, 2) NULL AFTER exam_remarks,
    ADD COLUMN interview_remarks VARCHAR(255) NULL AFTER interview_score,
    ADD COLUMN evaluated_by_user_id BIGINT NULL AFTER interview_remarks,
    ADD COLUMN interviewed_by_user_id BIGINT NULL AFTER evaluated_by_user_id,
    ADD CONSTRAINT fk_adm_evaluated_by FOREIGN KEY (evaluated_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_adm_interviewed_by FOREIGN KEY (interviewed_by_user_id) REFERENCES users (id) ON DELETE SET NULL;

-- 3. Seed Initial Admission Configuration for Term 4 (Locked by default until ADMIN/GUIDANCE activation)
INSERT INTO admission_configs (id, term_id, is_active, daily_slot_limit, total_opened_slots, days_open, start_date, end_date)
VALUES (1, 4, FALSE, 1000, 20000, 20, '2026-10-01', '2026-10-21')
AS new_row ON DUPLICATE KEY UPDATE 
    is_active = new_row.is_active,
    daily_slot_limit = new_row.daily_slot_limit,
    total_opened_slots = new_row.total_opened_slots;

SET FOREIGN_KEY_CHECKS = 1;
