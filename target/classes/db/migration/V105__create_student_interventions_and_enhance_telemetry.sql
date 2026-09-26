-- V105__create_student_interventions_and_enhance_telemetry.sql
-- Subsystem: Closed-Loop Intervention Case Management & Hardened Telemetry Architecture

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Create student_interventions table
CREATE TABLE IF NOT EXISTS student_interventions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_profile_id BIGINT NOT NULL,
    risk_score_id BIGINT NULL,
    intervention_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    assigned_counselor_id BIGINT NULL,
    trigger_factor VARCHAR(255) NOT NULL,
    case_notes TEXT NULL,
    resolution_summary TEXT NULL,
    dispatched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    CONSTRAINT fk_intervention_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_intervention_risk FOREIGN KEY (risk_score_id) REFERENCES student_risk_scores (id) ON DELETE SET NULL,
    CONSTRAINT fk_intervention_counselor FOREIGN KEY (assigned_counselor_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_interventions_student ON student_interventions (student_profile_id);
CREATE INDEX idx_interventions_status ON student_interventions (status);
CREATE INDEX idx_interventions_counselor ON student_interventions (assigned_counselor_id);

-- 2. Safely add secret_key column to attendance_sessions
SET @col_secret_key_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'attendance_sessions' 
      AND COLUMN_NAME = 'secret_key'
);

SET @sql_secret_key = IF(@col_secret_key_exists = 0,
    'ALTER TABLE attendance_sessions ADD COLUMN secret_key VARCHAR(64) NULL AFTER qr_seed',
    'SELECT 1'
);
PREPARE stmt_sk FROM @sql_secret_key;
EXECUTE stmt_sk;
DEALLOCATE PREPARE stmt_sk;

-- 3. Safely add index for device_fingerprint on attendance_records for anti-proxy lookups
SET @idx_device_exists = (
    SELECT COUNT(*) 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'attendance_records' 
      AND INDEX_NAME = 'idx_att_rec_device'
);

SET @sql_device_idx = IF(@idx_device_exists = 0,
    'CREATE INDEX idx_att_rec_device ON attendance_records (attendance_session_id, device_fingerprint)',
    'SELECT 1'
);
PREPARE stmt_di FROM @sql_device_idx;
EXECUTE stmt_di;
DEALLOCATE PREPARE stmt_di;

SET FOREIGN_KEY_CHECKS = 1;
