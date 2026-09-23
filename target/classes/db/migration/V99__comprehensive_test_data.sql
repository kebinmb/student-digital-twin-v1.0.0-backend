-- V99__comprehensive_test_data.sql
-- Subsystem: Master Test Suite Seed Data for End-to-End System Verification
-- Covers: 8 Institutional Roles, Academic Hierarchy, BSCS Curriculum with DAG Prerequisites,
--         Class Records, Financial Ledgers/Invoices/ORs, UniFAST Claims, Departmental Clearances,
--         Philippine Statutory Equity Profiles (RA 10931, RA 8371, RA 7277, RA 11861, GIDA),
--         Geofenced QR Attendance & Digital Twin Risk Telemetry.

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. SYSTEM USERS & ROLES
-- Password for all test accounts: Password123!
-- Argon2id hash: $argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE
-- -----------------------------------------------------------------------------
INSERT INTO users (id, username, college_id, program_id, email, password, enabled)
VALUES 
    (10, 'admin_sys', NULL, NULL, 'admin.sys@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (11, 'registrar_head', NULL, NULL, 'registrar.head@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (12, 'dean_eng', NULL, NULL, 'dean.eng@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (13, 'chair_cs', NULL, NULL, 'chair.cs@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (14, 'faculty_smith', NULL, NULL, 'faculty.smith@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (15, 'faculty_tan', NULL, NULL, 'faculty.tan@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (16, 'cashier_main', NULL, NULL, 'cashier@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (17, 'accountant_head', NULL, NULL, 'accountant@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (18, 'student_a', NULL, NULL, 'student.a@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (19, 'student_b', NULL, NULL, 'student.b@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (20, 'student_c', NULL, NULL, 'student.c@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE)
AS new_user ON DUPLICATE KEY UPDATE 
    email = new_user.email, 
    password = new_user.password;

INSERT INTO user_roles (user_id, role)
VALUES 
    (10, 'ADMIN'),
    (11, 'REGISTRAR'),
    (12, 'DEAN'),
    (13, 'CHAIRPERSON'),
    (14, 'FACULTY'),
    (15, 'FACULTY'),
    (16, 'CASHIER'),
    (17, 'ACCOUNTANT'),
    (18, 'STUDENT'),
    (19, 'STUDENT'),
    (20, 'STUDENT')
AS new_role ON DUPLICATE KEY UPDATE role = new_role.role;

-- -----------------------------------------------------------------------------
-- 2. ACADEMIC HIERARCHY & STRUCTURES
-- -----------------------------------------------------------------------------
INSERT INTO campuses (id, code, ched_institutional_code, name, address, region, contact_number, email, is_main, is_active)
VALUES (10, 'TALISAY_MAIN', 'CHMSU-06-001', 'CHMSU Talisay Main Campus', 'Talisay City, Negros Occidental', 'REGION_VI', '+63 (034) 712-0000', 'talisay.main@chmsu.edu.ph', TRUE, TRUE)
AS new_camp ON DUPLICATE KEY UPDATE name = new_camp.name;

INSERT INTO departments (id, campus_id, code, name, type, parent_department_id, dean_user_id, is_active)
VALUES (10, 10, 'CCS', 'College of Computer Studies', 'COLLEGE', NULL, 12, TRUE)
AS new_dept ON DUPLICATE KEY UPDATE name = new_dept.name;

INSERT INTO programs (id, department_id, college_id, chairperson_user_id, code, name, major, degree_level, ched_cmo_reference, government_permit, total_units_required, is_active)
VALUES (10, 10, 10, 13, 'BSCS', 'Bachelor of Science in Computer Science', 'Software Engineering', 'BACHELORS', 'CMO No. 25 s. 2015', 'GP-BSCS-2020-001', 140, TRUE)
AS new_prog ON DUPLICATE KEY UPDATE name = new_prog.name;

INSERT INTO academic_years (id, code, start_date, end_date, is_current)
VALUES (10, 'AY-2026-2027', '2026-08-01', '2027-07-31', TRUE)
AS new_ay ON DUPLICATE KEY UPDATE is_current = new_ay.is_current;
UPDATE academic_years SET is_current = FALSE WHERE id <> 10;

INSERT INTO terms (id, academic_year_id, term_type, start_date, end_date, enrollment_open, grading_open, add_drop_open, is_active, max_hours_per_class)
VALUES (10, 10, 'FIRST_SEM', '2026-08-15', '2026-12-20', TRUE, TRUE, TRUE, TRUE, 5)
AS new_term ON DUPLICATE KEY UPDATE enrollment_open = new_term.enrollment_open;
UPDATE terms SET is_active = FALSE WHERE id <> 10;

INSERT INTO rooms (id, campus_id, code, name, building, floor, capacity, room_type, is_active)
VALUES 
    (10, 10, 'CLAB-1', 'Computer Laboratory 1', 'CCS Building', 2, 40, 'LABORATORY', TRUE),
    (11, 10, 'CLAB-2', 'Computer Laboratory 2', 'CCS Building', 2, 40, 'LABORATORY', TRUE)
AS new_room ON DUPLICATE KEY UPDATE name = new_room.name;

-- -----------------------------------------------------------------------------
-- 3. CURRICULUM, COURSES & PREREQUISITES (DAG)
-- -----------------------------------------------------------------------------
INSERT INTO courses (id, code, title, lecture_units, lab_units, credit_units, contact_hours_lec, contact_hours_lab, category, description, is_active)
VALUES 
    (10, 'CS101', 'Introduction to Computer Science', 2.00, 1.00, 3.00, 2, 3, 'MAJOR', 'Fundamentals of computing, logic, and problem solving', TRUE),
    (11, 'CS102', 'Data Structures and Algorithms', 2.00, 1.00, 3.00, 2, 3, 'MAJOR', 'Arrays, linked lists, stacks, queues, trees, and graphs', TRUE),
    (12, 'CS201', 'Database Management Systems', 2.00, 1.00, 3.00, 2, 3, 'MAJOR', 'Relational data models, SQL, and database design', TRUE),
    (13, 'CS301', 'Software Engineering', 3.00, 0.00, 3.00, 3, 0, 'MAJOR', 'Software design lifecycle, design patterns, and Agile methodologies', TRUE)
AS new_course ON DUPLICATE KEY UPDATE title = new_course.title;

INSERT INTO curricula (id, program_id, code, name, status, version_number, effective_academic_year, is_active)
VALUES (10, 10, 'BSCS-2026', 'BSCS Curriculum 2026 Revision', 'APPROVED', 1, 'AY-2026-2027', TRUE)
AS new_curr ON DUPLICATE KEY UPDATE status = new_curr.status;

INSERT INTO curriculum_courses (id, curriculum_id, course_id, year_level, sequence_order, category, semester)
VALUES 
    (10, 10, 10, 1, 1, 'MAJOR', 'FIRST_SEMESTER'),
    (11, 10, 11, 1, 2, 'MAJOR', 'SECOND_SEMESTER'),
    (12, 10, 12, 2, 1, 'MAJOR', 'FIRST_SEMESTER'),
    (13, 10, 13, 3, 1, 'MAJOR', 'FIRST_SEMESTER')
AS new_curr_course ON DUPLICATE KEY UPDATE year_level = new_curr_course.year_level;

INSERT INTO course_prerequisites (id, course_id, prerequisite_course_id, rule_type, min_grade_required)
VALUES 
    (10, 11, 10, 'HARD_PREREQUISITE', 3.00), -- CS101 required before CS102
    (11, 12, 10, 'HARD_PREREQUISITE', 3.00)  -- CS101 required before CS201
AS new_prereq ON DUPLICATE KEY UPDATE min_grade_required = new_prereq.min_grade_required;

-- -----------------------------------------------------------------------------
-- 4. FACULTY & STUDENT PROFILES
-- -----------------------------------------------------------------------------
INSERT INTO faculty_profiles (id, user_id, college_id, program_id, faculty_id_number, highest_degree, academic_rank, prc_license_no, employment_status, is_tenured)
VALUES 
    (10, 14, 10, 10, 'FAC-2026-001', 'DOCTORATE', 'PROFESSOR_I', 'PRC-CS-9901', 'FULL_TIME', TRUE),
    (11, 15, 10, 10, 'FAC-2026-002', 'MASTERS', 'ASSOCIATE_PROFESSOR_I', 'PRC-CS-9902', 'FULL_TIME', TRUE)
AS new_fac ON DUPLICATE KEY UPDATE academic_rank = new_fac.academic_rank;

INSERT INTO student_profiles (id, user_id, student_number, program_id, curriculum_id, year_level, enrollment_status, student_classification, is_graduating, total_units_earned, cumulative_gpa, financial_clearance, departmental_clearance)
VALUES 
    (10, 18, '2026-0001', 10, 10, 3, 'REGULAR', 'CONTINUING', FALSE, 60.00, 1.25, 'CLEARED', 'CLEARED'),
    (11, 19, '2026-0002', 10, 10, 3, 'IRREGULAR', 'CONTINUING', FALSE, 54.00, 2.50, 'HOLD', 'CLEARED'),
    (12, 20, '2026-0003', 10, 10, 4, 'REGULAR', 'CONTINUING', TRUE, 120.00, 1.40, 'CLEARED', 'CLEARED')
AS new_stud ON DUPLICATE KEY UPDATE cumulative_gpa = new_stud.cumulative_gpa;

-- -----------------------------------------------------------------------------
-- 5. SECTIONS, SCHEDULES & ENROLLMENT
-- -----------------------------------------------------------------------------
INSERT INTO class_sections (id, term_id, curriculum_id, course_id, section_code, max_capacity, enrolled_count, status, grade_status, primary_instructor_id)
VALUES 
    (10, 10, 10, 11, 'BSCS-3A', 40, 3, 'OPEN', 'DRAFT', 14),
    (11, 10, 10, 12, 'BSCS-3B', 40, 3, 'OPEN', 'DRAFT', 15)
AS new_sec ON DUPLICATE KEY UPDATE section_code = new_sec.section_code;

INSERT INTO class_schedules (id, section_id, room_id, instructor_user_id, day_of_week, start_time, end_time, schedule_type)
VALUES 
    (10, 10, 10, 14, 'MONDAY', '08:00:00', '11:00:00', 'LABORATORY'),
    (11, 11, 11, 15, 'WEDNESDAY', '13:00:00', '16:00:00', 'LABORATORY')
AS new_sched ON DUPLICATE KEY UPDATE start_time = new_sched.start_time;

INSERT INTO student_enrollments (id, student_id, term_id, enrollment_date, status, total_credit_units, is_overload_approved)
VALUES 
    (10, 10, 10, '2026-08-16 09:00:00', 'ENROLLED', 6.00, FALSE),
    (11, 11, 10, '2026-08-16 10:30:00', 'ENROLLED', 6.00, FALSE),
    (12, 12, 10, '2026-08-16 11:15:00', 'ENROLLED', 6.00, FALSE)
AS new_enr ON DUPLICATE KEY UPDATE status = new_enr.status;

INSERT INTO enrollment_course_items (id, enrollment_id, section_id, final_numerical_grade, completion_status)
VALUES 
    (10, 10, 10, NULL, 'ENROLLED'),
    (11, 10, 11, NULL, 'ENROLLED'),
    (12, 11, 10, NULL, 'ENROLLED'),
    (13, 11, 11, NULL, 'ENROLLED'),
    (14, 12, 10, NULL, 'ENROLLED'),
    (15, 12, 11, NULL, 'ENROLLED')
AS new_item ON DUPLICATE KEY UPDATE completion_status = new_item.completion_status;

-- -----------------------------------------------------------------------------
-- 6. DYNAMIC CLASS RECORDS & ASSESSMENT SCORES
-- -----------------------------------------------------------------------------
INSERT INTO section_grading_configs (id, section_id, midterm_weight, final_weight, is_locked)
VALUES 
    (10, 10, 50.00, 50.00, FALSE),
    (11, 11, 50.00, 50.00, FALSE)
AS new_cfg ON DUPLICATE KEY UPDATE midterm_weight = new_cfg.midterm_weight;

INSERT INTO section_grading_categories (id, config_id, category_name, weight_percentage, term_period, display_order)
VALUES 
    (10, 10, 'Quizzes', 20.00, 'MIDTERM', 1),
    (11, 10, 'Machine Problems', 30.00, 'MIDTERM', 2),
    (12, 10, 'Midterm Exam', 50.00, 'MIDTERM', 3),
    (13, 11, 'Quizzes', 20.00, 'MIDTERM', 1),
    (14, 11, 'Machine Problems', 30.00, 'MIDTERM', 2),
    (15, 11, 'Midterm Exam', 50.00, 'MIDTERM', 3)
AS new_cat ON DUPLICATE KEY UPDATE weight_percentage = new_cat.weight_percentage;

INSERT INTO class_record_items (id, category_id, item_title, max_points, sequence_order)
VALUES 
    (10, 10, 'Quiz 1 - Binary Search Trees', 20.00, 1),
    (11, 11, 'MP 1 - AVL Tree Implementation', 50.00, 1),
    (12, 12, 'Midterm Examination', 100.00, 1),
    (13, 13, 'Quiz 1 - Relational Algebra', 20.00, 1),
    (14, 14, 'MP 1 - SQL Normalization', 50.00, 1),
    (15, 15, 'Midterm Examination', 100.00, 1)
AS new_cri ON DUPLICATE KEY UPDATE max_points = new_cri.max_points;

INSERT INTO student_assessment_scores (id, item_id, student_id, score_earned, is_excused)
VALUES 
    (10, 10, 10, 19.50, FALSE),
    (11, 11, 10, 48.00, FALSE),
    (12, 12, 10, 92.00, FALSE),
    (13, 10, 11, 12.00, FALSE),
    (14, 11, 11, 30.00, FALSE),
    (15, 12, 11, 65.00, FALSE),
    (16, 10, 12, 18.00, FALSE),
    (17, 11, 12, 45.00, FALSE),
    (18, 12, 12, 88.00, FALSE)
AS new_score ON DUPLICATE KEY UPDATE score_earned = new_score.score_earned;

-- -----------------------------------------------------------------------------
-- 7. FINANCIAL LEDGERS, INVOICES, RECEPTS & UNIFAST CLAIMS
-- -----------------------------------------------------------------------------
INSERT INTO fee_templates (id, name, academic_year_id, campus_id, tuition_per_unit, lab_fee_per_unit, miscellaneous_flat_fee, athletic_flat_fee, is_active)
VALUES (10, 'Standard BSCS Fee Template AY 2026-2027', 10, 10, 300.00, 500.00, 1500.00, 500.00, TRUE)
AS new_ft ON DUPLICATE KEY UPDATE tuition_per_unit = new_ft.tuition_per_unit;

INSERT INTO student_assessment_invoices (id, invoice_number, student_enrollment_id, student_profile_id, term_id, total_tuition_fee, total_lab_fee, total_misc_fee, total_gross_assessment, fhe_subsidy_amount, scholarship_discount_amount, net_assessed_amount, total_paid_amount, outstanding_balance, status, is_fhe_eligible)
VALUES 
    (10, 'INV-2026-0001', 10, 10, 10, 1800.00, 1000.00, 2000.00, 4800.00, 4800.00, 0.00, 0.00, 0.00, 0.00, 'FHE_COVERED', TRUE),
    (11, 'INV-2026-0002', 11, 11, 10, 1800.00, 1000.00, 2000.00, 4800.00, 0.00, 0.00, 4800.00, 1000.00, 3800.00, 'PARTIAL', FALSE),
    (12, 'INV-2026-0003', 12, 12, 10, 1800.00, 1000.00, 2000.00, 4800.00, 4800.00, 0.00, 0.00, 0.00, 0.00, 'FHE_COVERED', TRUE)
AS new_inv ON DUPLICATE KEY UPDATE outstanding_balance = new_inv.outstanding_balance;

INSERT INTO student_account_ledgers (id, transaction_number, student_profile_id, term_id, assessment_invoice_id, transaction_type, description, debit_amount, credit_amount, running_balance, reference_number, created_by_user_id)
VALUES 
    (10, 'TXN-2026-0001', 10, 10, 10, 'CHARGE', '1st Semester AY 2026-2027 Gross Assessment', 4800.00, 0.00, 4800.00, 'INV-2026-0001', 17),
    (11, 'TXN-2026-0002', 10, 10, 10, 'FHE_SUBSIDY', 'RA 10931 Free Higher Education Subsidy', 0.00, 4800.00, 0.00, 'FHE-2026-001', 17),
    (12, 'TXN-2026-0003', 11, 10, 11, 'CHARGE', '1st Semester AY 2026-2027 Gross Assessment', 4800.00, 0.00, 4800.00, 'INV-2026-0002', 17),
    (13, 'TXN-2026-0004', 11, 10, 11, 'PAYMENT', 'Cash Payment Downpayment', 0.00, 1000.00, 3800.00, 'OR-2026-0001', 16)
AS new_ledg ON DUPLICATE KEY UPDATE running_balance = new_ledg.running_balance;

INSERT INTO cashier_receipts (id, or_number, student_profile_id, assessment_invoice_id, amount_tendered, amount_paid, change_amount, payment_method, reference_number, remarks, status, cashier_user_id)
VALUES (10, 'OR-2026-0001', 11, 11, 1000.00, 1000.00, 0.00, 'CASH', 'REF-PAY-001', 'Partial enrollment payment', 'VALID', 16)
AS new_rcpt ON DUPLICATE KEY UPDATE amount_paid = new_rcpt.amount_paid;

INSERT INTO unifast_fhe_claims (id, claim_batch_number, term_id, campus_id, total_beneficiaries, total_tuition_claimed, total_tosf_claimed, total_claim_amount, status, created_by_user_id)
VALUES (10, 'UNIFAST-BATCH-2026-01', 10, 10, 2, 3600.00, 6000.00, 9600.00, 'SUBMITTED', 17)
AS new_claim ON DUPLICATE KEY UPDATE total_claim_amount = new_claim.total_claim_amount;

INSERT INTO unifast_fhe_claim_items (id, claim_batch_id, student_profile_id, assessment_invoice_id, enrolled_units, tuition_amount, misc_amount, lab_amount, total_claimed_amount, verification_status)
VALUES 
    (10, 10, 10, 10, 6.00, 1800.00, 2000.00, 1000.00, 4800.00, 'VERIFIED'),
    (11, 10, 12, 12, 6.00, 1800.00, 2000.00, 1000.00, 4800.00, 'VERIFIED')
AS new_citem ON DUPLICATE KEY UPDATE total_claimed_amount = new_citem.total_claimed_amount;

-- -----------------------------------------------------------------------------
-- 8. MULTI-DEPARTMENT CLEARANCES & GRADUATION AUDIT
-- -----------------------------------------------------------------------------
INSERT INTO clearance_requests (id, student_profile_id, term_id, purpose, overall_status)
VALUES 
    (10, 10, 10, 'ENROLLMENT', 'APPROVED'),
    (11, 11, 10, 'ENROLLMENT', 'REJECTED'),
    (12, 12, 10, 'GRADUATION', 'PENDING')
AS new_clr ON DUPLICATE KEY UPDATE overall_status = new_clr.overall_status;

INSERT INTO clearance_signoffs (id, clearance_request_id, department_type, signoff_status, remarks, signed_by_user_id, signed_at)
VALUES 
    (10, 10, 'LIBRARY', 'APPROVED', 'No unreturned books', 11, '2026-08-15 10:00:00'),
    (11, 10, 'CASHIER', 'APPROVED', 'Fully covered by UniFAST', 16, '2026-08-15 10:30:00'),
    (12, 11, 'CASHIER', 'REJECTED', 'Unpaid balance of P3,800.00', 16, '2026-08-15 11:00:00'),
    (13, 12, 'LIBRARY', 'APPROVED', 'Cleared for graduation', 11, '2026-08-15 14:00:00'),
    (14, 12, 'CASHIER', 'APPROVED', 'Zero balance', 16, '2026-08-15 14:30:00')
AS new_sign ON DUPLICATE KEY UPDATE signoff_status = new_sign.signoff_status;

INSERT INTO graduation_applications (id, student_profile_id, curriculum_id, term_id, application_date, degree_audit_status, total_units_completed, cumulative_gpa, honors_status, special_order_number)
VALUES (10, 12, 10, 10, '2026-08-01', 'QUALIFIED', 120.00, 1.40, 'MAGNA_CUM_LAUDE', 'SO-2026-CHED-0091')
AS new_grad ON DUPLICATE KEY UPDATE degree_audit_status = new_grad.degree_audit_status;

-- -----------------------------------------------------------------------------
-- 9. PHILIPPINE STATUTORY EQUITY TARGET PROFILING
-- -----------------------------------------------------------------------------
INSERT INTO student_equity_profiles (
    id, student_profile_id, 
    is_4ps_beneficiary, household_4ps_id_number, is_listahanan_nhts, unifast_tes_awardee, unifast_tes_award_number,
    is_indigenous_people, ip_ethnic_group, ncip_certificate_number,
    is_person_with_disability, pwd_id_number, disability_type,
    is_solo_parent, is_raised_by_solo_parent, solo_parent_id_number,
    is_orphan,
    is_gida_resident, gida_barangay_residence,
    is_farmer_fisherfolk, rsbsa_registration_number,
    is_rebel_returnee_family, certificate_of_surrender_number,
    is_bottom_40_income_bracket, monthly_household_income_bracket,
    is_first_generation_college,
    verification_status, verified_by_user_id, verified_at, verification_remarks
) VALUES 
    -- Student A: 4Ps / UniFAST TES Awardee / Listahanan / Poor / Bottom 40% / First-Gen
    (10, 10, 
     TRUE, '4PS-VI-109283-2020', TRUE, TRUE, 'TES-2026-09812', 
     FALSE, NULL, NULL, 
     FALSE, NULL, NULL, 
     FALSE, FALSE, NULL, 
     FALSE, 
     FALSE, NULL, 
     FALSE, NULL, 
     FALSE, NULL, 
     TRUE, 'POOR_BELOW_10K', 
     TRUE, 
     'VERIFIED', 11, '2026-08-15 09:00:00', 'Verified against DSWD Listahanan database'),

    -- Student B: Raised by Solo Parent / Low Income / Bottom 40%
    (11, 11, 
     FALSE, NULL, FALSE, FALSE, NULL, 
     FALSE, NULL, NULL, 
     FALSE, NULL, NULL, 
     FALSE, TRUE, 'SP-2024-5512', 
     FALSE, 
     FALSE, NULL, 
     FALSE, NULL, 
     FALSE, NULL, 
     TRUE, 'LOW_INCOME_10K_TO_20K', 
     FALSE, 
     'VERIFIED', 11, '2026-08-15 09:30:00', 'Verified via LGU Social Welfare Office ID'),

    -- Student C: PWD / First-Gen / GIDA Resident / Farmer-Fisherfolk
    (12, 12, 
     FALSE, NULL, FALSE, FALSE, NULL, 
     FALSE, NULL, NULL, 
     TRUE, 'PWD-NEGROS-8821', 'VISUAL', 
     FALSE, FALSE, NULL, 
     FALSE, 
     TRUE, 'Barangay Katilingban', 
     TRUE, 'RSBSA-06-45-0912', 
     FALSE, NULL, 
     TRUE, 'LOW_INCOME_10K_TO_20K', 
     TRUE, 
     'VERIFIED', 11, '2026-08-15 10:00:00', 'Verified via PWD ID, RSBSA & Barangay Certificate of Residency')
AS new_eq ON DUPLICATE KEY UPDATE verification_status = new_eq.verification_status;

-- -----------------------------------------------------------------------------
-- 10. DIGITAL TWIN TELEMETRY, GEOFENCED QR ATTENDANCE & ML RISK SCORES
-- CHMSU Geofence Target Coordinates: 10.69750000 N, 122.96440000 E (Allowed Radius: 50m)
-- -----------------------------------------------------------------------------
INSERT INTO attendance_sessions (id, section_schedule_id, session_date, qr_seed, qr_expires_at, latitude, longitude, allowed_radius_meters)
VALUES (10, 10, '2026-08-20', 'SEED-BSCS3A-20260820-001', '2026-08-20 08:30:00', 10.69750000, 122.96440000, 50)
AS new_att_sess ON DUPLICATE KEY UPDATE qr_seed = new_att_sess.qr_seed;

INSERT INTO attendance_records (id, attendance_session_id, student_profile_id, scanned_at, attendance_status, device_fingerprint, verified_latitude, verified_longitude)
VALUES 
    (10, 10, 10, '2026-08-20 08:05:12', 'PRESENT', 'FP-IPHONE-STUDENT-A', 10.69751000, 122.96441000),
    (11, 10, 11, '2026-08-20 08:28:45', 'LATE', 'FP-ANDROID-STUDENT-B', 10.69758000, 122.96445000),
    (12, 10, 12, '2026-08-20 08:02:10', 'PRESENT', 'FP-LAPTOP-STUDENT-C', 10.69749000, 122.96439000)
AS new_att_rec ON DUPLICATE KEY UPDATE attendance_status = new_att_rec.attendance_status;

INSERT INTO student_risk_scores (id, student_profile_id, evaluated_at, academic_risk_score, attendance_risk_score, socioeconomic_risk_score, composite_risk_level, predicted_dropout_probability, recommended_interventions)
VALUES 
    (10, 10, '2026-08-20 12:00:00', 0.10, 0.05, 0.40, 'LOW', 0.0450, 'Maintain regular academic advising and UniFAST monitoring.'),
    (11, 11, '2026-08-20 12:00:00', 0.65, 0.75, 0.80, 'HIGH', 0.7250, 'Flagged for urgent academic counseling and student financial assistance referral.'),
    (12, 12, '2026-08-20 12:00:00', 0.15, 0.10, 0.35, 'LOW', 0.0800, 'Clear for graduation degree audit process.')
AS new_risk ON DUPLICATE KEY UPDATE composite_risk_level = new_risk.composite_risk_level;

-- -----------------------------------------------------------------------------
-- 11. LMS LTI INTEROPERABILITY DEPLOYMENTS
-- -----------------------------------------------------------------------------
INSERT INTO lti_deployments (id, platform_name, client_id, deployment_id, oidc_auth_url, access_token_url, jwks_url, is_active)
VALUES (10, 'Canvas LMS - CHMSU Portal', 'canvas-client-id-chmsu-2026', 'dep-001-canvas-main', 'https://canvas.chmsu.edu.ph/api/lti/authorize', 'https://canvas.chmsu.edu.ph/login/oauth2/token', 'https://canvas.chmsu.edu.ph/api/lti/security/jwks', TRUE)
AS new_lti ON DUPLICATE KEY UPDATE platform_name = new_lti.platform_name;

INSERT INTO lti_user_mappings (id, lti_deployment_id, user_id, sub_claim)
VALUES 
    (10, 10, 14, 'canvas-sub-prof-smith'),
    (11, 10, 18, 'canvas-sub-student-a')
AS new_lti_map ON DUPLICATE KEY UPDATE sub_claim = new_lti_map.sub_claim;

-- -----------------------------------------------------------------------------
-- 12. MULTI-DEPARTMENT CLEARANCE REQUESTS & SIGNOFFS
-- -----------------------------------------------------------------------------
INSERT INTO clearance_requests (id, student_profile_id, term_id, purpose, overall_status)
VALUES 
    (10, 10, 10, 'GRADUATION', 'PENDING'),
    (11, 11, 10, 'TRANSFER', 'PENDING'),
    (12, 12, 10, 'GRADUATION', 'CLEARED')
AS new_clr ON DUPLICATE KEY UPDATE overall_status = new_clr.overall_status;

INSERT INTO clearance_signoffs (id, clearance_request_id, department_type, signoff_status, remarks)
VALUES 
    (101, 10, 'LIBRARY', 'APPROVED', 'No unreturned books'),
    (102, 10, 'ACCOUNTING', 'PENDING', NULL),
    (103, 10, 'LABORATORY', 'APPROVED', 'Equipment returned'),
    (104, 10, 'STUDENT_AFFAIRS', 'PENDING', NULL),
    (105, 10, 'DEAN', 'PENDING', NULL),
    
    (111, 11, 'LIBRARY', 'APPROVED', NULL),
    (112, 11, 'ACCOUNTING', 'REJECTED', 'Unpaid balance of P1,200.00'),
    (113, 11, 'LABORATORY', 'APPROVED', NULL),
    (114, 11, 'STUDENT_AFFAIRS', 'APPROVED', NULL),
    (115, 11, 'DEAN', 'PENDING', NULL),

    (121, 12, 'LIBRARY', 'APPROVED', 'Cleared'),
    (122, 12, 'ACCOUNTING', 'APPROVED', 'Cleared'),
    (123, 12, 'LABORATORY', 'APPROVED', 'Cleared'),
    (124, 12, 'STUDENT_AFFAIRS', 'APPROVED', 'Cleared'),
    (125, 12, 'DEAN', 'APPROVED', 'Cleared')
AS new_so ON DUPLICATE KEY UPDATE signoff_status = new_so.signoff_status;

SET FOREIGN_KEY_CHECKS = 1;
