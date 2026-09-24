-- V101__performance_composite_indexes.sql
-- Subsystem: High-Concurrency Enterprise Query Indexing & Filesort Elimination

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Accelerate Student Account Ledger history queries and running balance lookups without filesort
CREATE INDEX idx_ledger_student_ordered ON student_account_ledgers (student_profile_id, transaction_date, id);

-- 2. Accelerate Student Enrollment resolution for advising, tuition fee assessment, and clearance checks
CREATE INDEX idx_enrollment_student_term_status ON student_enrollments (student_id, term_id, status);

-- 3. Accelerate Student Profile multi-column search by student number, last name, and first name
CREATE INDEX idx_student_search_lookup ON student_profiles (student_number, last_name, first_name);

-- 4. Accelerate Grade Sealing Audit history verification by section and timestamp
CREATE INDEX idx_gsa_section_date ON grade_sealing_audits (section_id, sealed_at);

-- 5. Accelerate UniFAST FHE Claim item verification and summary reporting
CREATE INDEX idx_claim_item_batch_status ON unifast_fhe_claim_items (claim_batch_id, verification_status);

SET FOREIGN_KEY_CHECKS = 1;
