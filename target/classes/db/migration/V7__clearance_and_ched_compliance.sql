-- V7__clearance_and_ched_compliance.sql
-- Subsystem: Multi-Department Student Clearance, Graduation Degree Audit & CHED Compliance

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS clearance_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_profile_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    overall_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_clearance_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_clearance_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_clearance_student_term ON clearance_requests (student_profile_id, term_id);
CREATE INDEX idx_clearance_status ON clearance_requests (overall_status);

CREATE TABLE IF NOT EXISTS clearance_signoffs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clearance_request_id BIGINT NOT NULL,
    department_type VARCHAR(50) NOT NULL,
    signoff_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    remarks VARCHAR(255) NULL,
    signed_by_user_id BIGINT NULL,
    signed_at TIMESTAMP NULL,
    CONSTRAINT fk_signoff_request FOREIGN KEY (clearance_request_id) REFERENCES clearance_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_signoff_user FOREIGN KEY (signed_by_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_signoff_request ON clearance_signoffs (clearance_request_id);
CREATE INDEX idx_signoff_dept_status ON clearance_signoffs (department_type, signoff_status);

CREATE TABLE IF NOT EXISTS graduation_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_profile_id BIGINT NOT NULL,
    curriculum_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    application_date DATE NOT NULL,
    degree_audit_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_units_completed DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    cumulative_gpa DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    honors_status VARCHAR(50) NOT NULL DEFAULT 'NONE',
    special_order_number VARCHAR(100) NULL,
    special_order_issued_at DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_grad_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_grad_curriculum FOREIGN KEY (curriculum_id) REFERENCES curricula (id) ON DELETE RESTRICT,
    CONSTRAINT fk_grad_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_grad_student ON graduation_applications (student_profile_id);

SET FOREIGN_KEY_CHECKS = 1;
