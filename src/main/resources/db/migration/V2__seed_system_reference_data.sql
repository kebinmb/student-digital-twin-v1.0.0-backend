-- V2__seed_system_reference_data.sql
-- Subsystem: Core Reference & Master Lookup Data
-- Scope: Permissions, Grading Scales, Fee Categories, Fee Catalog, Scholarships, Payment Templates

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. System Permissions Reference
-- -----------------------------------------------------------------------------
INSERT INTO permissions (id, name, description)
VALUES (1, 'sys:admin:manage', 'Full system administration and configuration'),
       (2, 'academic:curriculum:manage', 'Manage programs, course catalogs, and CMO matrices'),
       (3, 'faculty:workload:assign', 'Assign faculty teaching loads and section schedules'),
       (4, 'student:profile:view', 'View core student biographical and academic profile'),
       (5, 'student:equity:manage', 'Access and update RA 10687 / RA 10931 equity and vulnerability data'),
       (6, 'enrollment:advising:process', 'Evaluate prerequisites and process student enrollment'),
       (7, 'assessment:unifast:bill', 'Generate CHED-UniFAST Free Higher Education (FHE) billing statements'),
       (8, 'grading:encode:submit', 'Encode and submit mid-term and final grades'),
       (9, 'grading:registrar:lock', 'Verify, transmute, and lock academic grades'),
       (10, 'records:tor:issue', 'Generate official Transcript of Records and honorable dismissals'),
       (11, 'ched:hemis:export', 'Extract Form E-1 through E-5 compliance reporting data')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

-- -----------------------------------------------------------------------------
-- 2. Academic Grading Scales (CHMSU Institutional Standard)
-- -----------------------------------------------------------------------------
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
ON DUPLICATE KEY UPDATE numeric_grade = VALUES(numeric_grade),
                        percentage_min = VALUES(percentage_min),
                        percentage_max = VALUES(percentage_max),
                        transmuted_grade = VALUES(transmuted_grade),
                        remarks = VALUES(remarks),
                        is_passing = VALUES(is_passing),
                        is_non_numeric = VALUES(is_non_numeric);

-- -----------------------------------------------------------------------------
-- 3. Fee Categories & Master Fee Catalog (RA 10931 FHE Compliant)
-- -----------------------------------------------------------------------------
INSERT INTO fee_categories (id, code, name)
VALUES (1, 'TUITION', 'Tuition per Credit Unit'),
       (2, 'MISC_MANDATORY', 'CHED-Approved Mandatory Miscellaneous Fees'),
       (3, 'LABORATORY', 'Laboratory and Hands-On Workshop Fees'),
       (4, 'OTHER_SCHOOL_FEES', 'Other Institutional / Non-FHE Fees')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO fee_catalog (id, category_id, code, name, default_amount, is_per_unit, is_ched_sanctioned, is_fhe_billable)
VALUES (1, 1, 'TUIT_UG', 'Tuition Fee - Undergraduate', 200.00, TRUE, TRUE, TRUE),
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
ON DUPLICATE KEY UPDATE category_id = VALUES(category_id),
                        name = VALUES(name),
                        default_amount = VALUES(default_amount),
                        is_per_unit = VALUES(is_per_unit),
                        is_ched_sanctioned = VALUES(is_ched_sanctioned),
                        is_fhe_billable = VALUES(is_fhe_billable);

-- -----------------------------------------------------------------------------
-- 4. Scholarship & Grant Templates
-- -----------------------------------------------------------------------------
INSERT INTO scholarship_discounts (id, code, name, type, category, discount_percentage, fixed_amount, applies_to_tuition, applies_to_misc, funding_source)
VALUES (1, 'UNIFAST_FHE', 'RA 10931 Free Higher Education (FHE)', 'UNIFAST_FHE', 'CHED_UNIFAST', 100.00, 0.00, TRUE, TRUE, 'CHED-UniFAST Central Office'),
       (2, 'CHED_TES', 'Tertiary Education Subsidy (TES / UniFAST)', 'CHED_TES', 'CHED_UNIFAST', 0.00, 20000.00, FALSE, FALSE, 'CHED-UniFAST Direct Stipend'),
       (3, 'CHED_TDP', 'CHED Tulong Dunong Program (TDP-TES)', 'CHED_TDP', 'CHED_UNIFAST', 0.00, 7500.00, FALSE, FALSE, 'CHED Regional Office VI'),
       (4, 'NOSP_SCHOLAR', 'Negros Occidental Scholarship Program (NOSP)', 'LGU_GRANT', 'GOVERNMENT_MANDATED', 0.00, 5000.00, FALSE, FALSE, 'Provincial Government of Negros Occidental')
ON DUPLICATE KEY UPDATE name = VALUES(name),
                        type = VALUES(type),
                        category = VALUES(category),
                        discount_percentage = VALUES(discount_percentage),
                        fixed_amount = VALUES(fixed_amount),
                        applies_to_tuition = VALUES(applies_to_tuition),
                        applies_to_misc = VALUES(applies_to_misc),
                        funding_source = VALUES(funding_source);

-- -----------------------------------------------------------------------------
-- 5. Tuition Payment Schedule Templates
-- -----------------------------------------------------------------------------
INSERT INTO payment_term_templates (id, name, downpayment_pct, prelim_pct, midterm_pct, semifinal_pct, final_pct)
VALUES (1, 'CHMSU UniFAST FHE Billing Matrix (Single Claim)', 0.00, 0.00, 0.00, 0.00, 100.00),
       (2, 'CHMSU Paying Students (3-Period Tranche)', 30.00, 0.00, 35.00, 0.00, 35.00)
ON DUPLICATE KEY UPDATE downpayment_pct = VALUES(downpayment_pct),
                        prelim_pct = VALUES(prelim_pct),
                        midterm_pct = VALUES(midterm_pct),
                        semifinal_pct = VALUES(semifinal_pct),
                        final_pct = VALUES(final_pct);

SET FOREIGN_KEY_CHECKS = 1;
