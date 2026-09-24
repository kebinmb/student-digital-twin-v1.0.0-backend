-- V17__suc_fund_clusters_and_tosf_master.sql
-- Subsystem: Philippine SUC Fund Clusters (GAM for SUCs) & 13 Authorized TOSF Categories (RA 10931)

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS fund_clusters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    bank_account_number VARCHAR(50),
    depository_bank_name VARCHAR(100) DEFAULT 'Land Bank of the Philippines',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO fund_clusters (id, code, name, description, bank_account_number, depository_bank_name, active)
VALUES (1, 'FUND_101', 'General Fund (Regular Agency Fund - MDS)', 'National Government Subsidy through DBM Modified Disbursement System', 'MDS-101-0001', 'Bureau of the Treasury / Land Bank of the Philippines', TRUE),
       (2, 'FUND_164', 'Special Trust Fund (Internally Generated Income)', 'SUC Revolving Fund (RA 8292) for Tuition, TOSF, and Internally Generated Income', '1422-00164-88', 'Land Bank of the Philippines', TRUE),
       (3, 'FUND_184', 'Trust Receipts (Fiduciary & Student Funds)', 'Student government fees, alumni trust deposits, laboratory breakage deposits', '1422-00184-99', 'Land Bank of the Philippines', TRUE)
AS new_fc ON DUPLICATE KEY UPDATE name = new_fc.name;

-- Alter cashier_receipts to add check details and fund cluster
ALTER TABLE cashier_receipts
    ADD COLUMN check_number VARCHAR(50) NULL AFTER reference_number,
    ADD COLUMN drawee_bank VARCHAR(100) NULL AFTER check_number,
    ADD COLUMN fund_cluster_code VARCHAR(20) NOT NULL DEFAULT 'FUND_164' AFTER cashier_user_id;

-- Alter student_account_ledgers to add fund cluster
ALTER TABLE student_account_ledgers
    ADD COLUMN fund_cluster_code VARCHAR(20) NOT NULL DEFAULT 'FUND_164' AFTER reference_number;

-- Add tosf_category_index and fund_cluster_code to fee_catalog
ALTER TABLE fee_catalog
    ADD COLUMN tosf_category_index INT NULL AFTER is_fhe_billable,
    ADD COLUMN fund_cluster_code VARCHAR(20) NOT NULL DEFAULT 'FUND_164' AFTER tosf_category_index;

-- Add indexes for SUC Fund Cluster queries
CREATE INDEX idx_cashier_receipts_fund_cluster ON cashier_receipts (fund_cluster_code);
CREATE INDEX idx_student_ledgers_fund_cluster ON student_account_ledgers (fund_cluster_code);
CREATE INDEX idx_fee_catalog_tosf ON fee_catalog (tosf_category_index);

-- Seed the 13 authorized categories of TOSF under RA 10931 Section 7
INSERT INTO fee_catalog (id, category_id, code, name, default_amount, is_per_unit, is_ched_sanctioned, is_fhe_billable, tosf_category_index, fund_cluster_code)
VALUES 
    (10, 2, 'TOSF_ADM', 'Admission Fees (Entrance Testing & Application)', 100.00, FALSE, TRUE, TRUE, 1, 'FUND_164'),
    (11, 2, 'TOSF_ENT', 'Entrance & Matriculation Fees', 150.00, FALSE, TRUE, TRUE, 2, 'FUND_164'),
    (12, 2, 'TOSF_REG', 'Registration & Enrollment Validation Fees', 150.00, FALSE, TRUE, TRUE, 3, 'FUND_164'),
    (13, 2, 'TOSF_MED', 'Medical and Dental Examination Fees', 100.00, FALSE, TRUE, TRUE, 4, 'FUND_164'),
    (14, 2, 'TOSF_LIB', 'Library & Research Database Subscriptions', 200.00, FALSE, TRUE, TRUE, 5, 'FUND_164'),
    (15, 3, 'TOSF_LAB', 'Applied Laboratory & Science Workshop Fees', 500.00, TRUE, TRUE, TRUE, 6, 'FUND_164'),
    (16, 2, 'TOSF_IT', 'Information Technology & Campus Network Fees', 300.00, FALSE, TRUE, TRUE, 7, 'FUND_164'),
    (17, 2, 'TOSF_ATH', 'Athletics & Sports Development Fees', 150.00, FALSE, TRUE, TRUE, 8, 'FUND_164'),
    (18, 2, 'TOSF_CUL', 'Cultural Development & Performing Arts Fees', 100.00, FALSE, TRUE, TRUE, 9, 'FUND_164'),
    (19, 2, 'TOSF_GUI', 'Guidance, Counseling & Testing Fees', 100.00, FALSE, TRUE, TRUE, 10, 'FUND_164'),
    (20, 2, 'TOSF_DEV', 'Development & Institutional Facilities Fees', 250.00, FALSE, TRUE, TRUE, 11, 'FUND_164'),
    (21, 2, 'TOSF_ID', 'Student Handbook and Smart ID Card Fees', 150.00, FALSE, TRUE, TRUE, 12, 'FUND_164'),
    (22, 2, 'TOSF_EXAM', 'Periodic Examination Permits & Test Booklets', 100.00, FALSE, TRUE, TRUE, 13, 'FUND_164')
AS new_tosf ON DUPLICATE KEY UPDATE name = new_tosf.name, tosf_category_index = new_tosf.tosf_category_index;

SET FOREIGN_KEY_CHECKS = 1;
