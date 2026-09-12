-- V9__digital_twin_telemetry.sql
-- Subsystem: Dynamic QR Attendance Telemetry & Digital Twin Early Warning ML Risk Scores

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS attendance_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_schedule_id BIGINT NOT NULL,
    session_date DATE NOT NULL,
    qr_seed VARCHAR(100) NOT NULL,
    qr_expires_at TIMESTAMP NOT NULL,
    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,
    allowed_radius_meters INT NOT NULL DEFAULT 50,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attsess_schedule FOREIGN KEY (section_schedule_id) REFERENCES class_schedules (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_attsess_schedule_date ON attendance_sessions (section_schedule_id, session_date);

CREATE TABLE IF NOT EXISTS attendance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attendance_session_id BIGINT NOT NULL,
    student_profile_id BIGINT NOT NULL,
    scanned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attendance_status VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    device_fingerprint VARCHAR(255) NULL,
    verified_latitude DECIMAL(10, 8) NULL,
    verified_longitude DECIMAL(11, 8) NULL,
    CONSTRAINT fk_attrec_session FOREIGN KEY (attendance_session_id) REFERENCES attendance_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_attrec_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT uq_attrec_session_student UNIQUE (attendance_session_id, student_profile_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_attrec_session ON attendance_records (attendance_session_id);
CREATE INDEX idx_attrec_student ON attendance_records (student_profile_id);

CREATE TABLE IF NOT EXISTS student_risk_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_profile_id BIGINT NOT NULL,
    evaluated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    academic_risk_score DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    attendance_risk_score DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    socioeconomic_risk_score DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    composite_risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    predicted_dropout_probability DECIMAL(5, 4) NOT NULL DEFAULT 0.0500,
    recommended_interventions TEXT NULL,
    CONSTRAINT fk_srs_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_srs_student ON student_risk_scores (student_profile_id);
CREATE INDEX idx_srs_risk_level ON student_risk_scores (composite_risk_level);

SET FOREIGN_KEY_CHECKS = 1;
