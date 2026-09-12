-- V6__financial_ledger_and_cashiering.sql
-- Subsystem: Financial Ledgers, Fee Catalog, Assessment Invoices, Cashier Receipts & UniFAST Claims

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS fee_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS fee_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    default_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    is_per_unit BOOLEAN NOT NULL DEFAULT FALSE,
    is_ched_sanctioned BOOLEAN NOT NULL DEFAULT TRUE,
    is_fhe_billable BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_fee_catalog_category FOREIGN KEY (category_id) REFERENCES fee_categories (id) ON DELETE CASCADE,
    CONSTRAINT chk_fee_amount_non_negative CHECK (default_amount >= 0.00)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS scholarship_discounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    category VARCHAR(30) NOT NULL DEFAULT 'INSTITUTIONAL',
    discount_percentage DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    fixed_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    applies_to_tuition BOOLEAN NOT NULL DEFAULT TRUE,
    applies_to_misc BOOLEAN NOT NULL DEFAULT TRUE,
    funding_source VARCHAR(100) NULL,
    CONSTRAINT chk_discount_pct CHECK (discount_percentage >= 0.00 AND discount_percentage <= 100.00),
    CONSTRAINT chk_fixed_amount CHECK (fixed_amount >= 0.00)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS payment_term_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    downpayment_pct DECIMAL(5, 2) NOT NULL,
    prelim_pct DECIMAL(5, 2) NOT NULL,
    midterm_pct DECIMAL(5, 2) NOT NULL,
    semifinal_pct DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    final_pct DECIMAL(5, 2) NOT NULL,
    CONSTRAINT chk_pay_term_total CHECK (downpayment_pct + prelim_pct + midterm_pct + semifinal_pct + final_pct = 100.00)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS fee_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    academic_year_id BIGINT NOT NULL,
    campus_id BIGINT NULL,
    tuition_per_unit DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    lab_fee_per_unit DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    miscellaneous_flat_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    athletic_flat_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_fee_templates_ay FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE CASCADE,
    CONSTRAINT fk_fee_templates_campus FOREIGN KEY (campus_id) REFERENCES campuses (id) ON DELETE SET NULL,
    CONSTRAINT chk_tuition_per_unit CHECK (tuition_per_unit >= 0.00),
    CONSTRAINT chk_lab_per_unit CHECK (lab_fee_per_unit >= 0.00),
    CONSTRAINT chk_misc_flat CHECK (miscellaneous_flat_fee >= 0.00),
    CONSTRAINT chk_athletic_flat CHECK (athletic_flat_fee >= 0.00)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_fee_templates_ay ON fee_templates (academic_year_id);
CREATE INDEX idx_fee_templates_campus ON fee_templates (campus_id);

CREATE TABLE IF NOT EXISTS student_assessment_invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    student_enrollment_id BIGINT NOT NULL,
    student_profile_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    total_tuition_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_lab_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_misc_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_gross_assessment DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    fhe_subsidy_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    scholarship_discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    net_assessed_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_paid_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    outstanding_balance DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    is_fhe_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_enrollment FOREIGN KEY (student_enrollment_id) REFERENCES student_enrollments (id) ON DELETE CASCADE,
    CONSTRAINT fk_invoice_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_invoice_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE CASCADE,
    CONSTRAINT chk_invoice_gross CHECK (total_gross_assessment >= 0.00),
    CONSTRAINT chk_invoice_net CHECK (net_assessed_amount >= 0.00)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_invoice_student_term ON student_assessment_invoices (student_profile_id, term_id);
CREATE INDEX idx_invoice_status ON student_assessment_invoices (status);

CREATE TABLE IF NOT EXISTS student_account_ledgers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_number VARCHAR(50) NOT NULL UNIQUE,
    student_profile_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    assessment_invoice_id BIGINT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255) NOT NULL,
    debit_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    credit_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    running_balance DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    reference_number VARCHAR(100) NULL,
    created_by_user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_ledger_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_ledger_invoice FOREIGN KEY (assessment_invoice_id) REFERENCES student_assessment_invoices (id) ON DELETE SET NULL,
    CONSTRAINT fk_ledger_creator FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_ledger_debit CHECK (debit_amount >= 0.00),
    CONSTRAINT chk_ledger_credit CHECK (credit_amount >= 0.00)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_ledger_student_term ON student_account_ledgers (student_profile_id, term_id);
CREATE INDEX idx_ledger_tx_type ON student_account_ledgers (transaction_type);

CREATE TABLE IF NOT EXISTS cashier_receipts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    or_number VARCHAR(50) NOT NULL UNIQUE,
    student_profile_id BIGINT NOT NULL,
    assessment_invoice_id BIGINT NULL,
    amount_tendered DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    amount_paid DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    change_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    payment_method VARCHAR(30) NOT NULL,
    reference_number VARCHAR(100) NULL,
    remarks VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VALID',
    cashier_user_id BIGINT NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    voided_at TIMESTAMP NULL,
    voided_by_user_id BIGINT NULL,
    void_reason VARCHAR(255) NULL,
    CONSTRAINT fk_receipt_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_invoice FOREIGN KEY (assessment_invoice_id) REFERENCES student_assessment_invoices (id) ON DELETE SET NULL,
    CONSTRAINT fk_receipt_cashier FOREIGN KEY (cashier_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_voider FOREIGN KEY (voided_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_receipt_paid CHECK (amount_paid >= 0.00)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_receipt_or_number ON cashier_receipts (or_number);
CREATE INDEX idx_receipt_student ON cashier_receipts (student_profile_id);
CREATE INDEX idx_receipt_cashier ON cashier_receipts (cashier_user_id);

CREATE TABLE IF NOT EXISTS unifast_fhe_claims (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    claim_batch_number VARCHAR(50) NOT NULL UNIQUE,
    term_id BIGINT NOT NULL,
    campus_id BIGINT NOT NULL,
    total_beneficiaries INT NOT NULL DEFAULT 0,
    total_tuition_claimed DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_tosf_claimed DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_claim_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    submitted_at TIMESTAMP NULL,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_unifast_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE CASCADE,
    CONSTRAINT fk_unifast_campus FOREIGN KEY (campus_id) REFERENCES campuses (id) ON DELETE CASCADE,
    CONSTRAINT fk_unifast_creator FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_unifast_total CHECK (total_claim_amount >= 0.00)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_unifast_batch ON unifast_fhe_claims (claim_batch_number);
CREATE INDEX idx_unifast_term_campus ON unifast_fhe_claims (term_id, campus_id);

CREATE TABLE IF NOT EXISTS unifast_fhe_claim_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    claim_batch_id BIGINT NOT NULL,
    student_profile_id BIGINT NOT NULL,
    assessment_invoice_id BIGINT NOT NULL,
    enrolled_units DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    tuition_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    misc_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    lab_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_claimed_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'VERIFIED',
    CONSTRAINT fk_claim_item_batch FOREIGN KEY (claim_batch_id) REFERENCES unifast_fhe_claims (id) ON DELETE CASCADE,
    CONSTRAINT fk_claim_item_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_claim_item_invoice FOREIGN KEY (assessment_invoice_id) REFERENCES student_assessment_invoices (id) ON DELETE CASCADE,
    CONSTRAINT chk_claim_item_total CHECK (total_claimed_amount >= 0.00)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_claim_item_batch_student ON unifast_fhe_claim_items (claim_batch_id, student_profile_id);

-- Seed Financial Fee Categories, Catalog, Scholarships & Payment Templates
INSERT INTO fee_categories (id, code, name)
VALUES (1, 'TUITION', 'Tuition per Credit Unit'),
       (2, 'MISC_MANDATORY', 'CHED-Approved Mandatory Miscellaneous Fees'),
       (3, 'LABORATORY', 'Laboratory and Hands-On Workshop Fees'),
       (4, 'OTHER_SCHOOL_FEES', 'Other Institutional / Non-FHE Fees')
AS new_fc ON DUPLICATE KEY UPDATE name = new_fc.name;

INSERT INTO fee_catalog (id, category_id, code, name, default_amount, is_per_unit, is_ched_sanctioned, is_fhe_billable)
VALUES (1, 1, 'TUIT_UG', 'Tuition Fee - Undergraduate', 200.00, TRUE, TRUE, TRUE),
       (2, 2, 'MISC_LIB', 'Library Fee', 200.00, FALSE, TRUE, TRUE),
       (3, 2, 'MISC_ATH', 'Athletics & Sports Development Fee', 150.00, FALSE, TRUE, TRUE),
       (4, 2, 'MISC_MED', 'Medical & Dental Examination Fee', 100.00, FALSE, TRUE, TRUE),
       (5, 2, 'MISC_REG', 'Registration & Processing Fee', 150.00, FALSE, TRUE, TRUE),
       (6, 3, 'LAB_COMP', 'Computer Laboratory Hands-on Fee', 500.00, FALSE, TRUE, TRUE),
       (7, 3, 'LAB_SCI', 'Natural Science & Chemistry Lab Fee', 450.00, FALSE, TRUE, TRUE),
       (8, 4, 'OTHER_ID', 'RFID Student Identification Smartcard', 250.00, FALSE, FALSE, FALSE)
AS new_fcat ON DUPLICATE KEY UPDATE default_amount = new_fcat.default_amount;

INSERT INTO scholarship_discounts (id, code, name, type, category, discount_percentage, fixed_amount, applies_to_tuition, applies_to_misc, funding_source)
VALUES (1, 'SCH_VAL', 'Valedictorian Full Academic Honor Scholarship', 'PERCENTAGE', 'INSTITUTIONAL', 100.00, 0.00, TRUE, TRUE, 'CHMSU Institutional Honor Fund'),
       (2, 'SCH_SAL', 'Salutatorian Half Academic Honor Scholarship', 'PERCENTAGE', 'INSTITUTIONAL', 50.00, 0.00, TRUE, FALSE, 'CHMSU Institutional Honor Fund'),
       (3, 'TES_UNIFAST', 'UniFAST Tertiary Education Subsidy (RA 10931)', 'FIXED_AMOUNT', 'NATIONAL_GOVT', 0.00, 20000.00, TRUE, TRUE, 'CHED-UniFAST National Subsidy'),
       (4, 'TDP_CHED', 'Tulong Dunong Program (CHED TDP)', 'FIXED_AMOUNT', 'NATIONAL_GOVT', 0.00, 7500.00, TRUE, TRUE, 'CHED Regional Office VI')
AS new_sd ON DUPLICATE KEY UPDATE discount_percentage = new_sd.discount_percentage;

INSERT INTO payment_term_templates (id, name, downpayment_pct, prelim_pct, midterm_pct, semifinal_pct, final_pct)
VALUES (1, 'STANDARD_4_TRANCHE', 25.00, 25.00, 25.00, 0.00, 25.00),
       (2, 'STANDARD_5_TRANCHE', 20.00, 20.00, 20.00, 20.00, 20.00),
       (3, 'FULL_PAYMENT_DISCOUNT', 100.00, 0.00, 0.00, 0.00, 0.00)
AS new_ptt ON DUPLICATE KEY UPDATE downpayment_pct = new_ptt.downpayment_pct;

INSERT INTO fee_templates (id, name, academic_year_id, campus_id, tuition_per_unit, lab_fee_per_unit, miscellaneous_flat_fee, athletic_flat_fee, is_active)
VALUES (1, 'AY 2024-2025 Undergraduate Fee Template', 2, 1, 200.00, 500.00, 600.00, 150.00, TRUE)
AS new_ft ON DUPLICATE KEY UPDATE tuition_per_unit = new_ft.tuition_per_unit;

SET FOREIGN_KEY_CHECKS = 1;
