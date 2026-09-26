-- V108__create_faculty_attendance_records.sql
-- Subsystem: Dynamic QR Attendance Telemetry - Separate Faculty & Session Conductor Attendance Storage

SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS upgrade_v108_attendance_sessions;

DELIMITER $$
CREATE PROCEDURE upgrade_v108_attendance_sessions()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'attendance_sessions' 
          AND COLUMN_NAME = 'creator_user_id'
    ) THEN
        ALTER TABLE attendance_sessions
            ADD COLUMN creator_user_id BIGINT NULL,
            ADD CONSTRAINT fk_attsess_creator FOREIGN KEY (creator_user_id) REFERENCES users (id) ON DELETE SET NULL;
    END IF;
END$$
DELIMITER ;

CALL upgrade_v108_attendance_sessions();
DROP PROCEDURE IF EXISTS upgrade_v108_attendance_sessions;

CREATE TABLE IF NOT EXISTS faculty_attendance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attendance_session_id BIGINT NOT NULL,
    faculty_user_id BIGINT NOT NULL,
    faculty_profile_id BIGINT NULL,
    verified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attendance_status VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    device_fingerprint VARCHAR(255) NULL,
    verified_latitude DECIMAL(10, 8) NULL,
    verified_longitude DECIMAL(11, 8) NULL,
    is_geofence_valid TINYINT(1) NOT NULL DEFAULT 1,
    notes VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_facatt_session FOREIGN KEY (attendance_session_id) REFERENCES attendance_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_facatt_user FOREIGN KEY (faculty_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_facatt_profile FOREIGN KEY (faculty_profile_id) REFERENCES faculty_profiles (id) ON DELETE SET NULL,
    CONSTRAINT uq_facatt_session_user UNIQUE (attendance_session_id, faculty_user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
