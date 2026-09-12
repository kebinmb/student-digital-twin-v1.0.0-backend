-- V6__create_phase4_financial_management.sql
-- Subsystem: Financial & Fee Management (Phase 4 Domain 04)
-- Scope: Tuition Fee Templates, Assessment Invoices, Account Ledgers, Cashier Receipts, and UniFAST FHE Claims

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Fee Templates (Base Tuition & Rates per Academic Period)
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
    CONSTRAINT fk_fee_templates_campus FOREIGN KEY (campus_id) REFERENCES campuses (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_fee_templates_ay ON fee_templates (academic_year_id);
CREATE INDEX idx_fee_templates_campus ON fee_templates (campus_id);

-- 2. Student Assessment Invoices
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
    CONSTRAINT fk_invoice_term FOREIGN KEY (term_id) REFERENCES terms (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_invoice_student_term ON student_assessment_invoices (student_profile_id, term_id);
CREATE INDEX idx_invoice_status ON student_assessment_invoices (status);

-- 3. Student Account Ledgers (Double-Entry Ledger)
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
    CONSTRAINT fk_ledger_creator FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_ledger_student_term ON student_account_ledgers (student_profile_id, term_id);
CREATE INDEX idx_ledger_tx_type ON student_account_ledgers (transaction_type);

-- 4. Cashier Official Receipts
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
    CONSTRAINT fk_receipt_voider FOREIGN KEY (voided_by_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_receipt_or_number ON cashier_receipts (or_number);
CREATE INDEX idx_receipt_student ON cashier_receipts (student_profile_id);
CREATE INDEX idx_receipt_cashier ON cashier_receipts (cashier_user_id);

-- 5. UniFAST FHE Claims Batch & Items
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
    CONSTRAINT fk_unifast_creator FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE RESTRICT
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
    CONSTRAINT fk_claim_item_invoice FOREIGN KEY (assessment_invoice_id) REFERENCES student_assessment_invoices (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_claim_item_batch_student ON unifast_fhe_claim_items (claim_batch_id, student_profile_id);

SET FOREIGN_KEY_CHECKS = 1;
