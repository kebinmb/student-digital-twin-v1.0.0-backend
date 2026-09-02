-- V4__phase1_master_setup.sql
-- Phase 1 Foundation Layer Master Setup: Table Definitions (DDL) and CHMSU Baseline Seed Data
-- References: CHED Institutional Profile (Region VI), CHMSU Campuses, RA 10931 (FHE SUC billing)

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. Table Definitions (DDL)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permissions (
    role VARCHAR(30) NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role, permission_id),
    CONSTRAINT fk_role_permissions_perm FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campuses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    ched_institutional_code VARCHAR(20),
    name VARCHAR(100) NOT NULL,
    address TEXT,
    region VARCHAR(50) NOT NULL DEFAULT 'REGION VI',
    contact_number VARCHAR(30),
    email VARCHAR(100),
    is_main BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campus_id BIGINT NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'COLLEGE',
    parent_department_id BIGINT,
    dean_user_id BIGINT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_campus_dept_code UNIQUE (campus_id, code),
    CONSTRAINT fk_departments_campus FOREIGN KEY (campus_id) REFERENCES campuses (id) ON DELETE CASCADE,
    CONSTRAINT fk_departments_parent FOREIGN KEY (parent_department_id) REFERENCES departments (id) ON DELETE SET NULL,
    CONSTRAINT fk_departments_dean FOREIGN KEY (dean_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS academic_years (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS terms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    term_type VARCHAR(20) NOT NULL,
    term_name VARCHAR(50),
    start_date DATE,
    end_date DATE,
    enrollment_open BOOLEAN NOT NULL DEFAULT FALSE,
    grading_open BOOLEAN NOT NULL DEFAULT FALSE,
    add_drop_open BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_ay_term UNIQUE (academic_year_id, term_type),
    CONSTRAINT fk_terms_ay FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS grading_scales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    numeric_grade DECIMAL(3, 2),
    percentage_min DECIMAL(5, 2) NOT NULL,
    percentage_max DECIMAL(5, 2) NOT NULL,
    transmuted_grade VARCHAR(10),
    remarks VARCHAR(50) NOT NULL,
    is_passing BOOLEAN NOT NULL DEFAULT TRUE,
    is_non_numeric BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fee_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fee_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    default_amount DECIMAL(10, 2) NOT NULL,
    is_per_unit BOOLEAN NOT NULL DEFAULT FALSE,
    is_ched_sanctioned BOOLEAN NOT NULL DEFAULT TRUE,
    is_fhe_billable BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_fee_catalog_category FOREIGN KEY (category_id) REFERENCES fee_categories (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS scholarship_discounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    category VARCHAR(30) NOT NULL DEFAULT 'INSTITUTIONAL',
    discount_percentage DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    fixed_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    applies_to_tuition BOOLEAN NOT NULL DEFAULT TRUE,
    applies_to_misc BOOLEAN NOT NULL DEFAULT TRUE,
    funding_source VARCHAR(100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_term_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    downpayment_pct DECIMAL(5, 2) NOT NULL,
    prelim_pct DECIMAL(5, 2) NOT NULL,
    midterm_pct DECIMAL(5, 2) NOT NULL,
    semifinal_pct DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    final_pct DECIMAL(5, 2) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. Seed Data
-- -----------------------------------------------------------------------------
INSERT INTO permissions (name, description)
VALUES ('sys:admin:manage', 'Full system administration and configuration'),
       ('academic:curriculum:manage', 'Manage programs, course catalogs, and CMO matrices'),
       ('faculty:workload:assign', 'Assign faculty teaching loads and section schedules'),
       ('student:profile:view', 'View core student biographical and academic profile'),
       ('student:equity:manage', 'Access and update RA 10687 / RA 10931 equity and vulnerability data'),
       ('enrollment:advising:process', 'Evaluate prerequisites and process student enrollment'),
       ('assessment:unifast:bill', 'Generate CHED-UniFAST Free Higher Education (FHE) billing statements'),
       ('grading:encode:submit', 'Encode and submit mid-term and final grades'),
       ('grading:registrar:lock', 'Verify, transmute, and lock academic grades'),
       ('records:tor:issue', 'Generate official Transcript of Records and honorable dismissals'),
       ('ched:hemis:export', 'Extract Form E-1 through E-5 compliance reporting data')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO campuses (id, code, ched_institutional_code, name, address, region, contact_number, email, is_main, is_active)
VALUES (1, 'TALISAY', '06014', 'CHMSU - Talisay (Main Campus)', 'Mabini St., Brgy. Zone 1, Talisay City, Negros Occidental', 'REGION VI', '(034) 712-0005', 'info@chmsu.edu.ph', TRUE, TRUE),
       (2, 'ALIIJIS', '06015', 'CHMSU - Alijis Campus', 'Brgy. Alijis, Bacolod City, Negros Occidental', 'REGION VI', '(034) 434-4043', 'alijis.director@chmsu.edu.ph', FALSE, TRUE),
       (3, 'BINALBAGAN', '06016', 'CHMSU - Binalbagan Campus', 'Brgy. Enclaro, Binalbagan, Negros Occidental', 'REGION VI', '(034) 388-8264', 'binalbagan.campus@chmsu.edu.ph', FALSE, TRUE),
       (4, 'FORTUNE', '06017', 'CHMSU - Fortune Towne Campus', 'Brgy. Estefania, Bacolod City, Negros Occidental', 'REGION VI', '(034) 433-5210', 'fortunetowne@chmsu.edu.ph', FALSE, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO departments (id, campus_id, code, name, type, parent_department_id, is_active)
VALUES 
(1, 1, 'OUR', 'Office of the University Registrar', 'ADMINISTRATIVE', NULL, TRUE),
(2, 1, 'SASO', 'Student Affairs and Services Office', 'ADMINISTRATIVE', NULL, TRUE),
(3, 1, 'COED', 'College of Education', 'COLLEGE', NULL, TRUE),
(4, 1, 'CAS', 'College of Arts and Sciences', 'COLLEGE', NULL, TRUE),
(5, 1, 'CCE', 'College of Civil Engineering & Technology', 'COLLEGE', NULL, TRUE),
(6, 2, 'CIT', 'College of Industrial Technology', 'COLLEGE', NULL, TRUE),
(7, 2, 'CCS', 'College of Computer Studies', 'COLLEGE', NULL, TRUE),
(8, 3, 'CFAS', 'College of Fisheries and Allied Sciences', 'COLLEGE', NULL, TRUE),
(9, 3, 'CBMA_B', 'College of Business Management and Accountancy - Bin', 'COLLEGE', NULL, TRUE),
(10, 4, 'CBMA_FT', 'College of Business Management and Accountancy', 'COLLEGE', NULL, TRUE),
(11, 4, 'CPAG', 'College of Public Administration and Governance', 'COLLEGE', NULL, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO departments (id, campus_id, code, name, type, parent_department_id, is_active)
VALUES (12, 2, 'IT_DEPT', 'Department of Information Technology', 'DEPARTMENT', 7, TRUE),
       (13, 2, 'IS_DEPT', 'Department of Information Systems', 'DEPARTMENT', 7, TRUE),
       (14, 1, 'SED_DEPT', 'Department of Secondary Education', 'DEPARTMENT', 3, TRUE),
       (15, 1, 'MATH_DEPT', 'Department of Mathematics and Natural Sciences', 'DEPARTMENT', 4, TRUE),
       (16, 4, 'ACTG_DEPT', 'Department of Accountancy', 'DEPARTMENT', 10, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO academic_years (id, code, start_date, end_date, is_current)
VALUES (1, 'AY 2025-2026', '2025-08-11', '2026-07-17', FALSE),
       (2, 'AY 2026-2027', '2026-08-10', '2027-07-16', TRUE)
ON DUPLICATE KEY UPDATE is_current = VALUES(is_current);

INSERT INTO terms (id, academic_year_id, term_type, term_name, start_date, end_date, enrollment_open, grading_open, add_drop_open, is_active)
VALUES (1, 2, '1ST_SEM', '1st Semester AY 2026-2027', '2026-08-10', '2026-12-18', TRUE, FALSE, TRUE, TRUE),
       (2, 2, '2ND_SEM', '2nd Semester AY 2026-2027', '2027-01-18', '2027-05-28', FALSE, FALSE, FALSE, FALSE),
       (3, 2, 'SUMMER', 'Midyear Term 2027', '2027-06-14', '2027-07-23', FALSE, FALSE, FALSE, FALSE)
ON DUPLICATE KEY UPDATE term_name = VALUES(term_name);

INSERT INTO grading_scales (id, code, numeric_grade, percentage_min, percentage_max, transmuted_grade, remarks, is_passing, is_non_numeric)
VALUES (1, '1.00', 1.00, 97.00, 100.00, '1.00', 'EXCELLENT', TRUE, FALSE),
       (2, '1.25', 1.25, 94.00, 96.99, '1.25', 'SUPERIOR', TRUE, FALSE),
       (3, '1.50', 1.50, 91.00, 93.99, '1.50', 'VERY GOOD', TRUE, FALSE),
       (4, '1.75', 1.75, 88.00, 90.99, '1.75', 'GOOD', TRUE, FALSE),
       (5, '2.00', 2.00, 85.00, 87.99, '2.00', 'VERY SATISFACTORY', TRUE, FALSE),
       (6, '2.25', 2.25, 82.00, 84.99, '2.25', 'SATISFACTORY', TRUE, FALSE),
       (7, '2.50', 2.50, 79.00, 81.99, '2.50', 'FAIR', TRUE, FALSE),
       (8, '2.75', 2.75, 76.00, 78.99, '2.75', 'PASSING', TRUE, FALSE),
       (9, '3.00', 3.00, 75.00, 75.99, '3.00', 'PASSED BARELY', TRUE, FALSE),
       (10, '5.00', 5.00, 0.00, 74.99, '5.00', 'FAILED', FALSE, FALSE),
       (11, 'INC', NULL, 0.00, 0.00, 'INC', 'INCOMPLETE', FALSE, TRUE),
       (12, 'DRP', NULL, 0.00, 0.00, 'DRP', 'DROPPED', FALSE, TRUE)
ON DUPLICATE KEY UPDATE remarks = VALUES(remarks);

INSERT INTO fee_categories (id, code, name)
VALUES (1, 'TUITION', 'Tuition per Credit Unit'),
       (2, 'MISC_MANDATORY', 'CHED-Approved Mandatory Miscellaneous Fees'),
       (3, 'LABORATORY', 'Laboratory and Hands-On Workshop Fees'),
       (4, 'OTHER_SCHOOL_FEES', 'Other Institutional / Non-FHE Fees')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO fee_catalog (id, category_id, code, name, default_amount, is_per_unit, is_ched_sanctioned, is_fhe_billable)
VALUES
(1, 1, 'TUIT_UG', 'Tuition Fee - Undergraduate', 200.00, TRUE, TRUE, TRUE),
(2, 2, 'MISC_LIB', 'Library Fee', 200.00, FALSE, TRUE, TRUE),
(3, 2, 'MISC_ATH', 'Athletic Fee', 150.00, FALSE, TRUE, TRUE),
(4, 2, 'MISC_MED', 'Medical and Dental Fee', 100.00, FALSE, TRUE, TRUE),
(5, 2, 'MISC_REG', 'Registration Fee', 50.00, FALSE, TRUE, TRUE),
(6, 2, 'MISC_CULT', 'Cultural Activities Fee', 50.00, FALSE, TRUE, TRUE),
(7, 2, 'MISC_SC', 'Supreme Student Government (SSG) Fee', 60.00, FALSE, TRUE, TRUE),
(8, 2, 'MISC_PUB', 'Official Student Publication Fee', 75.00, FALSE, TRUE, TRUE),
(9, 3, 'LAB_COMP', 'Computer Laboratory Fee', 300.00, FALSE, TRUE, TRUE),
(10, 3, 'LAB_INDTECH', 'Industrial Technology Shop Fee', 400.00, FALSE, TRUE, TRUE),
(11, 3, 'LAB_NATSCI', 'Natural Sciences Wet Lab Fee', 250.00, FALSE, TRUE, TRUE),
(12, 4, 'OTH_ID_LOST', 'Replacement RFID Student ID', 250.00, FALSE, TRUE, FALSE),
(13, 4, 'OTH_TOR', 'Transcript of Records (Per Page/Copy)', 50.00, FALSE, TRUE, FALSE)
ON DUPLICATE KEY UPDATE default_amount = VALUES(default_amount);

INSERT INTO scholarship_discounts (id, code, name, type, category, discount_percentage, fixed_amount, applies_to_tuition, applies_to_misc, funding_source)
VALUES
(1, 'UNIFAST_FHE', 'RA 10931 Free Higher Education (FHE)', 'UNIFAST_FHE', 'CHED_UNIFAST', 100.00, 0.00, TRUE, TRUE, 'CHED-UniFAST Central Office'),
(2, 'CHED_TES', 'Tertiary Education Subsidy (TES / UniFAST)', 'CHED_TES', 'CHED_UNIFAST', 0.00, 20000.00, FALSE, FALSE, 'CHED-UniFAST Direct Stipend'),
(3, 'CHED_TDP', 'CHED Tulong Dunong Program (TDP-TES)', 'CHED_TDP', 'CHED_UNIFAST', 0.00, 7500.00, FALSE, FALSE, 'CHED Regional Office VI'),
(4, 'NOSP_SCHOLAR', 'Negros Occidental Scholarship Program (NOSP)', 'LGU_GRANT', 'GOVERNMENT_MANDATED', 0.00, 5000.00, FALSE, FALSE, 'Provincial Government of Negros Occidental')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO payment_term_templates (id, name, downpayment_pct, prelim_pct, midterm_pct, semifinal_pct, final_pct)
VALUES
(1, 'CHMSU UniFAST FHE Billing Matrix (Single Claim)', 0.00, 0.00, 0.00, 0.00, 100.00),
(2, 'CHMSU Paying Students (3-Period Tranche)', 30.00, 0.00, 35.00, 0.00, 35.00)
ON DUPLICATE KEY UPDATE name = VALUES(name);

SET FOREIGN_KEY_CHECKS = 1;