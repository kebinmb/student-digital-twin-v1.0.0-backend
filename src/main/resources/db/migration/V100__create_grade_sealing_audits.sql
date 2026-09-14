-- V100__create_grade_sealing_audits.sql
-- Subsystem: Registrar Grade Sealing Audit Ledger

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS grade_sealing_audits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id BIGINT NOT NULL,
    registrar_user_id BIGINT NOT NULL,
    student_records_sealed INT NOT NULL,
    section_code VARCHAR(30) NOT NULL,
    course_code VARCHAR(30) NOT NULL,
    sealed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checksum_hash VARCHAR(64) NULL,
    CONSTRAINT fk_gsa_section FOREIGN KEY (section_id) REFERENCES class_sections (id) ON DELETE CASCADE,
    CONSTRAINT fk_gsa_registrar FOREIGN KEY (registrar_user_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_gsa_section ON grade_sealing_audits (section_id);
CREATE INDEX idx_gsa_registrar ON grade_sealing_audits (registrar_user_id);

SET FOREIGN_KEY_CHECKS = 1;
