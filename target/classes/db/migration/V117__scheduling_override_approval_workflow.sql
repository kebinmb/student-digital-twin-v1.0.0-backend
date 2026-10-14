-- V117__scheduling_override_approval_workflow.sql
-- Subsystem: Scheduling Override Approval Workflow & Schedule Locking Mechanism
-- Purpose: Support cross-program and cross-college faculty teaching assignments with multi-level approval governance
-- Standard Compliance: MySQL 8.0 / H2 / InnoDB / utf8mb4

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Extend class_schedules with is_locked and lock_reason
ALTER TABLE class_schedules
    ADD COLUMN is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN lock_reason VARCHAR(50) DEFAULT 'NONE';

CREATE INDEX idx_class_schedules_locked ON class_schedules (is_locked);

-- 2. Create scheduling_override_requests table
CREATE TABLE IF NOT EXISTS scheduling_override_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id BIGINT NOT NULL,
    schedule_id BIGINT NULL,
    faculty_user_id BIGINT NOT NULL,
    faculty_profile_id BIGINT NULL,
    requested_by_user_id BIGINT NOT NULL,
    override_type VARCHAR(30) NOT NULL,
    home_program_id BIGINT NULL,
    home_college_id BIGINT NULL,
    target_program_id BIGINT NOT NULL,
    target_college_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    justification TEXT NOT NULL,
    resolution_notes TEXT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    CONSTRAINT fk_sor_section FOREIGN KEY (section_id) REFERENCES class_sections (id) ON DELETE CASCADE,
    CONSTRAINT fk_sor_schedule FOREIGN KEY (schedule_id) REFERENCES class_schedules (id) ON DELETE SET NULL,
    CONSTRAINT fk_sor_faculty_user FOREIGN KEY (faculty_user_id) REFERENCES users (id),
    CONSTRAINT fk_sor_faculty_profile FOREIGN KEY (faculty_profile_id) REFERENCES faculty_profiles (id) ON DELETE SET NULL,
    CONSTRAINT fk_sor_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_sor_home_program FOREIGN KEY (home_program_id) REFERENCES programs (id) ON DELETE SET NULL,
    CONSTRAINT fk_sor_home_college FOREIGN KEY (home_college_id) REFERENCES departments (id) ON DELETE SET NULL,
    CONSTRAINT fk_sor_target_program FOREIGN KEY (target_program_id) REFERENCES programs (id),
    CONSTRAINT fk_sor_target_college FOREIGN KEY (target_college_id) REFERENCES departments (id)
);

CREATE INDEX idx_sor_section ON scheduling_override_requests (section_id);
CREATE INDEX idx_sor_faculty ON scheduling_override_requests (faculty_user_id);
CREATE INDEX idx_sor_status ON scheduling_override_requests (status);
CREATE INDEX idx_sor_home_prog ON scheduling_override_requests (home_program_id);
CREATE INDEX idx_sor_home_coll ON scheduling_override_requests (home_college_id);

-- 3. Create scheduling_override_approvals table
CREATE TABLE IF NOT EXISTS scheduling_override_approvals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    approval_type VARCHAR(30) NOT NULL,
    step_order INT NOT NULL,
    required_role VARCHAR(30) NOT NULL,
    assigned_approver_user_id BIGINT NULL,
    decision_by_user_id BIGINT NULL,
    status VARCHAR(30) NOT NULL,
    decision_at TIMESTAMP NULL,
    remarks TEXT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_soa_request FOREIGN KEY (request_id) REFERENCES scheduling_override_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_soa_assigned_user FOREIGN KEY (assigned_approver_user_id) REFERENCES users (id),
    CONSTRAINT fk_soa_decision_user FOREIGN KEY (decision_by_user_id) REFERENCES users (id)
);

CREATE INDEX idx_soa_request ON scheduling_override_approvals (request_id);
CREATE INDEX idx_soa_approver ON scheduling_override_approvals (assigned_approver_user_id);
CREATE INDEX idx_soa_status ON scheduling_override_approvals (status);

SET FOREIGN_KEY_CHECKS = 1;
