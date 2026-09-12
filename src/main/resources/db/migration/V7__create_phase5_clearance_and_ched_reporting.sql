-- File: src/main/resources/db/migration/V7__create_phase5_clearance_and_ched_reporting.sql
-- Flyway Migration V7: Phase 5 Student Clearance, Graduation & CHED Regulatory Compliance

CREATE TABLE clearance_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_profile_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    overall_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_clearance_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles(id),
    CONSTRAINT fk_clearance_term FOREIGN KEY (term_id) REFERENCES terms(id)
);

CREATE TABLE clearance_signoffs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clearance_request_id BIGINT NOT NULL,
    department_type VARCHAR(50) NOT NULL,
    signoff_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    remarks VARCHAR(255),
    signed_by_user_id BIGINT,
    signed_at TIMESTAMP,
    CONSTRAINT fk_signoff_request FOREIGN KEY (clearance_request_id) REFERENCES clearance_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_signoff_user FOREIGN KEY (signed_by_user_id) REFERENCES users(id)
);

CREATE TABLE graduation_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_profile_id BIGINT NOT NULL,
    curriculum_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    application_date DATE NOT NULL,
    degree_audit_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_units_completed DECIMAL(5,2) DEFAULT 0.00,
    cumulative_gpa DECIMAL(4,2) DEFAULT 0.00,
    honors_status VARCHAR(50) DEFAULT 'NONE',
    special_order_number VARCHAR(100),
    special_order_issued_at DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_grad_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles(id),
    CONSTRAINT fk_grad_curriculum FOREIGN KEY (curriculum_id) REFERENCES curricula(id),
    CONSTRAINT fk_grad_term FOREIGN KEY (term_id) REFERENCES terms(id)
);
