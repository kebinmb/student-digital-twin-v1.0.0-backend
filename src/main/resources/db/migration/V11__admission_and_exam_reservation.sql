-- V11__admission_and_exam_reservation.sql
-- Subsystem: Public Guest Admissions, Entrance Exam Slot Reservation, Concurrency Queuing & CHED Personal Profiling

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Extend student_profiles with explicit name fields
ALTER TABLE student_profiles
    ADD COLUMN first_name VARCHAR(50) NULL AFTER student_number,
    ADD COLUMN middle_name VARCHAR(50) NULL AFTER first_name,
    ADD COLUMN last_name VARCHAR(50) NULL AFTER middle_name,
    ADD COLUMN suffix VARCHAR(10) NULL AFTER last_name;

-- 2. Entrance Exam Slot Schedule Table
CREATE TABLE IF NOT EXISTS entrance_exam_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term_id BIGINT NOT NULL,
    exam_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    venue_room VARCHAR(100) NOT NULL,
    max_capacity INT NOT NULL DEFAULT 50,
    reserved_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_term_exam_date_time_venue UNIQUE (term_id, exam_date, start_time, venue_room),
    CONSTRAINT fk_exam_slots_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_exam_slot_term_status ON entrance_exam_slots (term_id, status);

-- 3. Public Admission Applications Table (CHED & Data Privacy Act Compliant)
CREATE TABLE IF NOT EXISTS admission_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_number VARCHAR(30) NOT NULL UNIQUE,
    target_program_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    exam_slot_id BIGINT NULL,
    
    -- Personal Data
    first_name VARCHAR(50) NOT NULL,
    middle_name VARCHAR(50) NULL,
    last_name VARCHAR(50) NOT NULL,
    suffix VARCHAR(10) NULL,
    birth_date DATE NOT NULL,
    gender VARCHAR(20) NOT NULL DEFAULT 'FEMALE',
    civil_status VARCHAR(20) NOT NULL DEFAULT 'SINGLE',
    citizenship VARCHAR(50) NOT NULL DEFAULT 'FILIPINO',
    mobile_number VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,

    -- Academic Background & Form 137 Details
    lrn_number VARCHAR(30) NULL,
    high_school_name VARCHAR(150) NOT NULL,
    high_school_type VARCHAR(30) NOT NULL DEFAULT 'PUBLIC',
    shs_track_and_strand VARCHAR(100) NULL,
    high_school_gwa DECIMAL(4, 2) NULL,

    -- Address
    street_address VARCHAR(255) NOT NULL,
    barangay VARCHAR(100) NOT NULL,
    city_municipality VARCHAR(100) NOT NULL,
    province VARCHAR(100) NOT NULL,
    zip_code VARCHAR(10) NULL,

    -- Emergency Contact
    emergency_contact_name VARCHAR(150) NOT NULL,
    emergency_contact_relationship VARCHAR(50) NOT NULL,
    emergency_contact_number VARCHAR(20) NOT NULL,
    emergency_contact_email VARCHAR(100) NULL,

    -- Philippine Statutory Equity Indicators
    is_4ps_beneficiary BOOLEAN NOT NULL DEFAULT FALSE,
    household_4ps_id_number VARCHAR(60) NULL,
    is_indigenous_people BOOLEAN NOT NULL DEFAULT FALSE,
    ip_ethnic_group VARCHAR(100) NULL,
    ncip_certificate_number VARCHAR(100) NULL,
    is_person_with_disability BOOLEAN NOT NULL DEFAULT FALSE,
    disability_type VARCHAR(60) NULL,
    pwd_id_number VARCHAR(60) NULL,
    is_solo_parent BOOLEAN NOT NULL DEFAULT FALSE,
    is_raised_by_solo_parent BOOLEAN NOT NULL DEFAULT FALSE,
    solo_parent_id_number VARCHAR(60) NULL,
    is_orphan BOOLEAN NOT NULL DEFAULT FALSE,
    is_gida_resident BOOLEAN NOT NULL DEFAULT FALSE,
    gida_barangay_residence VARCHAR(150) NULL,
    is_farmer_fisherfolk BOOLEAN NOT NULL DEFAULT FALSE,
    rsbsa_registration_number VARCHAR(60) NULL,
    is_rebel_returnee_family BOOLEAN NOT NULL DEFAULT FALSE,
    certificate_of_surrender_number VARCHAR(60) NULL,
    is_bottom_40_income_bracket BOOLEAN NOT NULL DEFAULT FALSE,
    monthly_household_income_bracket VARCHAR(50) NOT NULL DEFAULT 'POOR_BELOW_10K',
    is_first_generation_college BOOLEAN NOT NULL DEFAULT FALSE,

    -- Queue & Concurrency Management Token
    queue_token VARCHAR(100) NULL,
    queue_position INT NULL,

    -- Lifecycle Status
    application_status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_adm_program FOREIGN KEY (target_program_id) REFERENCES programs (id) ON DELETE RESTRICT,
    CONSTRAINT fk_adm_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_adm_exam_slot FOREIGN KEY (exam_slot_id) REFERENCES entrance_exam_slots (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_adm_app_number ON admission_applications (application_number);
CREATE INDEX idx_adm_email ON admission_applications (email);
CREATE INDEX idx_adm_term_status ON admission_applications (term_id, application_status);

-- Seed Initial Open Entrance Exam Slots for Active Term (Term 4)
INSERT INTO entrance_exam_slots (id, term_id, exam_date, start_time, end_time, venue_room, max_capacity, reserved_count, status)
VALUES 
(1, 4, '2026-10-15', '08:30:00', '11:30:00', 'University Auditorium A', 100, 5, 'OPEN'),
(2, 4, '2026-10-15', '13:30:00', '16:30:00', 'University Auditorium B', 100, 2, 'OPEN'),
(3, 4, '2026-10-16', '08:30:00', '11:30:00', 'Testing Center Hall 1', 80, 0, 'OPEN')
AS new_slot ON DUPLICATE KEY UPDATE status = new_slot.status;

SET FOREIGN_KEY_CHECKS = 1;
