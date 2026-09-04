-- V11__add_faculty_preparations_and_scheduling_caps.sql
-- Phase 3 Scheduling Enhancements: Faculty Preparations, Custom Load Caps, and Class Session Limits

ALTER TABLE faculty_workloads ADD COLUMN number_of_preparations INT DEFAULT 0 NOT NULL;
ALTER TABLE faculty_workloads ADD COLUMN custom_max_load_units DECIMAL(4,2) NULL;
ALTER TABLE faculty_workloads ADD COLUMN override_reason VARCHAR(255) NULL;
ALTER TABLE faculty_workloads ADD COLUMN overridden_by_user_id BIGINT NULL;
ALTER TABLE faculty_workloads ADD CONSTRAINT fk_workload_overridden_by FOREIGN KEY (overridden_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE terms ADD COLUMN max_hours_per_class DECIMAL(3,1) DEFAULT 3.0 NOT NULL;
