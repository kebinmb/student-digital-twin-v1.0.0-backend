-- V99__comprehensive_test_data.sql
-- Subsystem: Master Institutional Test Suite Dynamic Seed Data for End-to-End System Verification
-- Aligned with: CHED CMO Policies & Standards, RA 10931 (FHE), COA Circulars (Form 51 e-OR),
--               DBM/GAM SUC Fund Clusters (101, 164, 184), 3-Tier OBE (IILO/PILO/CILO),
--               and Multi-Department Student Clearances.
--
-- Features:
-- 1. All timestamps and dates are calculated dynamically relative to execution time:
--    - Active Academic Term & Year: CURRENT_DATE anchored
--    - Live Unexpired Attendance QR Sessions: DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 2 HOUR)
--    - Active Admission Application Windows: DATE_SUB(CURRENT_DATE, 15) to DATE_ADD(CURRENT_DATE, 45)
--    - Future Entrance Exam Slots: DATE_ADD(CURRENT_DATE, INTERVAL 7/14/21 DAY)
--    - Chronologically coherent audit trails for Cashiering, Ledgers, Invoices, and Grade Sealing
-- 2. Foreign keys dynamically resolved via natural business key subqueries:
--    - Resilience across clean databases or existing populated states
-- 3. Strict alignment with JPA Entity Enums:
--    - StudentClassification: INCOMING_FIRST_YEAR, TRANSFEREE, RETURNEE, CONTINUING
--    - ApplicationStatus: ENROLLED, SUBMITTED, ELIGIBLE_FOR_ENROLLMENT, etc.
--    - RiskLevel: LOW, MODERATE, HIGH, CRITICAL
--    - Curriculum Status: ACTIVE
--    - 5-Department Clearance Signoffs: LIBRARY, ACCOUNTING, LABORATORY, STUDENT_AFFAIRS, DEAN
-- 4. 13 Degree Programs aligned with CHED CMO:
--    - BSCS, BSIT, BSIS (CMO 25 s. 2015)
--    - BSCpE (CMO 87 s. 2017), BSECE (CMO 101 s. 2017), BSCE (CMO 92 s. 2017)
--    - BSBA-FM (CMO 17 s. 2017), BSA (CMO 27 s. 2017), BSHM (CMO 62 s. 2017)
--    - BSED-MATH (CMO 75 s. 2017), BSED-ENG (CMO 75 s. 2017), BEED (CMO 74 s. 2017)
--    - BSCRIM (CMO 05 s. 2018)
--    - General Education Core (CMO 20 s. 2013 - 36 units)

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- IDEMPOTENT DDL GUARD: Grade Sealing Audits
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS grade_sealing_audits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id BIGINT NOT NULL,
    registrar_user_id BIGINT NOT NULL,
    student_records_sealed INT NOT NULL,
    section_code VARCHAR(30) NOT NULL,
    course_code VARCHAR(30) NOT NULL,
    sealed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checksum_hash VARCHAR(64) NULL,
    CONSTRAINT fk_gsa_section FOREIGN KEY (section_id) REFERENCES class_sections (id) ON DELETE CASCADE,
    CONSTRAINT fk_gsa_registrar FOREIGN KEY (registrar_user_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- 1. SYSTEM USERS & RBAC ROLES
-- Default password for all test accounts: Password123!
-- Argon2id hash: $argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE
-- -----------------------------------------------------------------------------
INSERT INTO users (id, username, college_id, program_id, email, password, enabled)
VALUES 
    -- Administrative & Regulatory Heads
    (10, 'admin_sys', NULL, NULL, 'admin.sys@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (21, 'admin_aux', NULL, NULL, 'admin.aux@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (11, 'registrar_head', NULL, NULL, 'registrar.head@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (22, 'registrar_evaluator', NULL, NULL, 'evaluator.reg@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),

    -- Academic Deans
    (12, 'dean_ccs', NULL, NULL, 'dean.ccs@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (23, 'dean_coe', NULL, NULL, 'dean.coe@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (24, 'dean_cbma', NULL, NULL, 'dean.cbma@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (25, 'dean_coed', NULL, NULL, 'dean.coed@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (26, 'dean_ccj', NULL, NULL, 'dean.ccj@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),

    -- Department Chairpersons
    (13, 'chair_cs', NULL, NULL, 'chair.cs@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (27, 'chair_it', NULL, NULL, 'chair.it@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (28, 'chair_cpe', NULL, NULL, 'chair.cpe@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (29, 'chair_fm', NULL, NULL, 'chair.fm@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (30, 'chair_seced', NULL, NULL, 'chair.seced@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (31, 'chair_crim', NULL, NULL, 'chair.crim@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),

    -- Faculty Members
    (14, 'faculty_smith', NULL, NULL, 'faculty.smith@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (15, 'faculty_tan', NULL, NULL, 'faculty.tan@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (32, 'faculty_garcia', NULL, NULL, 'faculty.garcia@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (33, 'faculty_reyes', NULL, NULL, 'faculty.reyes@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (34, 'faculty_delacruz', NULL, NULL, 'faculty.delacruz@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (35, 'faculty_villanueva', NULL, NULL, 'faculty.villanueva@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (36, 'faculty_aquino', NULL, NULL, 'faculty.aquino@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (37, 'faculty_mendoza', NULL, NULL, 'faculty.mendoza@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),

    -- Financial & Cashiering Personnel
    (16, 'cashier_main', NULL, NULL, 'cashier.main@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (38, 'cashier_ft', NULL, NULL, 'cashier.ft@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (17, 'accountant_head', NULL, NULL, 'accountant.head@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (39, 'accountant_billing', NULL, NULL, 'accountant.billing@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),

    -- Student Personas (15 diverse academic cases)
    (18, 'student_a', NULL, NULL, 'student.a@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (19, 'student_b', NULL, NULL, 'student.b@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (20, 'student_c', NULL, NULL, 'student.c@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (41, 'student_d', NULL, NULL, 'student.d@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (42, 'student_e', NULL, NULL, 'student.e@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (43, 'student_f', NULL, NULL, 'student.f@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (44, 'student_g', NULL, NULL, 'student.g@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (45, 'student_h', NULL, NULL, 'student.h@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (46, 'student_i', NULL, NULL, 'student.i@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (47, 'student_j', NULL, NULL, 'student.j@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (48, 'student_k', NULL, NULL, 'student.k@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (49, 'student_l', NULL, NULL, 'student.l@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (50, 'student_m', NULL, NULL, 'student.m@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (51, 'student_n', NULL, NULL, 'student.n@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
    (52, 'student_o', NULL, NULL, 'student.o@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE)
AS new_user ON DUPLICATE KEY UPDATE 
    email = new_user.email, 
    password = new_user.password,
    enabled = new_user.enabled;

INSERT INTO user_roles (user_id, role)
VALUES 
    ((SELECT id FROM users WHERE username = 'admin_sys' LIMIT 1), 'ADMIN'),
    ((SELECT id FROM users WHERE username = 'admin_aux' LIMIT 1), 'ADMIN'),
    ((SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), 'REGISTRAR'),
    ((SELECT id FROM users WHERE username = 'registrar_evaluator' LIMIT 1), 'REGISTRAR'),
    ((SELECT id FROM users WHERE username = 'dean_ccs' LIMIT 1), 'DEAN'),
    ((SELECT id FROM users WHERE username = 'dean_coe' LIMIT 1), 'DEAN'),
    ((SELECT id FROM users WHERE username = 'dean_cbma' LIMIT 1), 'DEAN'),
    ((SELECT id FROM users WHERE username = 'dean_coed' LIMIT 1), 'DEAN'),
    ((SELECT id FROM users WHERE username = 'dean_ccj' LIMIT 1), 'DEAN'),
    ((SELECT id FROM users WHERE username = 'chair_cs' LIMIT 1), 'CHAIRPERSON'),
    ((SELECT id FROM users WHERE username = 'chair_it' LIMIT 1), 'CHAIRPERSON'),
    ((SELECT id FROM users WHERE username = 'chair_cpe' LIMIT 1), 'CHAIRPERSON'),
    ((SELECT id FROM users WHERE username = 'chair_fm' LIMIT 1), 'CHAIRPERSON'),
    ((SELECT id FROM users WHERE username = 'chair_seced' LIMIT 1), 'CHAIRPERSON'),
    ((SELECT id FROM users WHERE username = 'chair_crim' LIMIT 1), 'CHAIRPERSON'),
    ((SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1), 'FACULTY'),
    ((SELECT id FROM users WHERE username = 'faculty_tan' LIMIT 1), 'FACULTY'),
    ((SELECT id FROM users WHERE username = 'faculty_garcia' LIMIT 1), 'FACULTY'),
    ((SELECT id FROM users WHERE username = 'faculty_reyes' LIMIT 1), 'FACULTY'),
    ((SELECT id FROM users WHERE username = 'faculty_delacruz' LIMIT 1), 'FACULTY'),
    ((SELECT id FROM users WHERE username = 'faculty_villanueva' LIMIT 1), 'FACULTY'),
    ((SELECT id FROM users WHERE username = 'faculty_aquino' LIMIT 1), 'FACULTY'),
    ((SELECT id FROM users WHERE username = 'faculty_mendoza' LIMIT 1), 'FACULTY'),
    ((SELECT id FROM users WHERE username = 'cashier_main' LIMIT 1), 'CASHIER'),
    ((SELECT id FROM users WHERE username = 'cashier_ft' LIMIT 1), 'CASHIER'),
    ((SELECT id FROM users WHERE username = 'accountant_head' LIMIT 1), 'ACCOUNTANT'),
    ((SELECT id FROM users WHERE username = 'accountant_billing' LIMIT 1), 'ACCOUNTANT'),
    ((SELECT id FROM users WHERE username = 'student_a' LIMIT 1), 'STUDENT'),
    ((SELECT id FROM users WHERE username = 'student_b' LIMIT 1), 'STUDENT'),
    ((SELECT id FROM users WHERE username = 'student_c' LIMIT 1), 'STUDENT'),
    ((SELECT id FROM users WHERE username = 'student_d' LIMIT 1), 'STUDENT'),
    ((SELECT id FROM users WHERE username = 'student_e' LIMIT 1), 'STUDENT'),
    ((SELECT id FROM users WHERE username = 'student_f' LIMIT 1), 'STUDENT'),
    ((SELECT id FROM users WHERE username = 'student_g' LIMIT 1), 'STUDENT'),
    ((SELECT id FROM users WHERE username = 'student_h' LIMIT 1), 'STUDENT'),
    ((SELECT id FROM users WHERE username = 'student_i' LIMIT 1), 'STUDENT'),
    ((SELECT id FROM users WHERE username = 'student_j' LIMIT 1), 'STUDENT'),
    ((SELECT id FROM users WHERE username = 'student_k' LIMIT 1), 'STUDENT'),
    ((SELECT id FROM users WHERE username = 'student_l' LIMIT 1), 'STUDENT'),
    ((SELECT id FROM users WHERE username = 'student_m' LIMIT 1), 'STUDENT'),
    ((SELECT id FROM users WHERE username = 'student_n' LIMIT 1), 'STUDENT'),
    ((SELECT id FROM users WHERE username = 'student_o' LIMIT 1), 'STUDENT')
AS new_role ON DUPLICATE KEY UPDATE role = new_role.role;

-- -----------------------------------------------------------------------------
-- 2. ACADEMIC HIERARCHY, SUC CAMPUSES & ROOM INFRASTRUCTURE
-- -----------------------------------------------------------------------------
INSERT INTO campuses (id, code, ched_institutional_code, name, address, region, contact_number, email, is_main, is_active)
VALUES 
    (10, 'TALISAY_MAIN', 'CHMSU-06-001', 'CHMSU Talisay Main Campus', 'Mabini St., Talisay City, Negros Occidental', 'REGION_VI', '+63 (034) 712-0000', 'talisay.main@chmsu.edu.ph', TRUE, TRUE),
    (11, 'FORTUNE_TOWNE', 'CHMSU-06-002', 'CHMSU Fortune Towne Campus', 'Brgy. Estefania, Bacolod City, Negros Occidental', 'REGION_VI', '+63 (034) 434-1234', 'fortunetowne@chmsu.edu.ph', FALSE, TRUE),
    (12, 'BINALBAGAN', 'CHMSU-06-003', 'CHMSU Binalbagan Campus', 'Brgy. Enclaro, Binalbagan, Negros Occidental', 'REGION_VI', '+63 (034) 388-5678', 'binalbagan@chmsu.edu.ph', FALSE, TRUE),
    (13, 'ALIJIS', 'CHMSU-06-004', 'CHMSU Alijis Campus', 'Brgy. Alijis, Bacolod City, Negros Occidental', 'REGION_VI', '+63 (034) 432-8899', 'alijis@chmsu.edu.ph', FALSE, TRUE)
AS new_camp ON DUPLICATE KEY UPDATE name = new_camp.name, is_main = new_camp.is_main;

INSERT INTO departments (id, campus_id, code, name, type, parent_department_id, dean_user_id, is_active)
VALUES 
    (10, (SELECT id FROM campuses WHERE code = 'TALISAY_MAIN' LIMIT 1), 'CCS', 'College of Computer Studies', 'COLLEGE', NULL, (SELECT id FROM users WHERE username = 'dean_ccs' LIMIT 1), TRUE),
    (11, (SELECT id FROM campuses WHERE code = 'TALISAY_MAIN' LIMIT 1), 'COE', 'College of Engineering', 'COLLEGE', NULL, (SELECT id FROM users WHERE username = 'dean_coe' LIMIT 1), TRUE),
    (12, (SELECT id FROM campuses WHERE code = 'FORTUNE_TOWNE' LIMIT 1), 'CBMA', 'College of Business Management and Accountancy', 'COLLEGE', NULL, (SELECT id FROM users WHERE username = 'dean_cbma' LIMIT 1), TRUE),
    (13, (SELECT id FROM campuses WHERE code = 'TALISAY_MAIN' LIMIT 1), 'COED', 'College of Education', 'COLLEGE', NULL, (SELECT id FROM users WHERE username = 'dean_coed' LIMIT 1), TRUE),
    (14, (SELECT id FROM campuses WHERE code = 'BINALBAGAN' LIMIT 1), 'CCJ', 'College of Criminal Justice', 'COLLEGE', NULL, (SELECT id FROM users WHERE username = 'dean_ccj' LIMIT 1), TRUE),
    (15, (SELECT id FROM campuses WHERE code = 'TALISAY_MAIN' LIMIT 1), 'CAS', 'College of Arts and Sciences', 'COLLEGE', NULL, NULL, TRUE)
AS new_dept ON DUPLICATE KEY UPDATE name = new_dept.name, dean_user_id = new_dept.dean_user_id;

-- Seed All 13 CHED CMO Degree Programs
INSERT INTO programs (id, department_id, college_id, chairperson_user_id, code, name, major, degree_level, ched_cmo_reference, government_permit, total_units_required, is_active)
VALUES 
    (10, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_cs' LIMIT 1), 'BSCS', 'Bachelor of Science in Computer Science', 'Software Engineering & Data Science', 'UNDERGRADUATE', 'CMO No. 25 s. 2015', 'GP-BSCS-2020-001', 140, TRUE),
    (11, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_it' LIMIT 1), 'BSIT', 'Bachelor of Science in Information Technology', 'Network & Web Systems', 'UNDERGRADUATE', 'CMO No. 25 s. 2015', 'GP-BSIT-2020-002', 146, TRUE),
    (12, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_cs' LIMIT 1), 'BSIS', 'Bachelor of Science in Information Systems', 'Enterprise Information Systems', 'UNDERGRADUATE', 'CMO No. 25 s. 2015', 'GP-BSIS-2020-003', 140, TRUE),
    (13, (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_cpe' LIMIT 1), 'BSCpE', 'Bachelor of Science in Computer Engineering', 'Embedded Systems & IoT', 'UNDERGRADUATE', 'CMO No. 87 s. 2017', 'GP-BSCE-2020-004', 160, TRUE),
    (14, (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_cpe' LIMIT 1), 'BSECE', 'Bachelor of Science in Electronics Engineering', 'Telecommunications', 'UNDERGRADUATE', 'CMO No. 101 s. 2017', 'GP-BECE-2020-005', 164, TRUE),
    (15, (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_cpe' LIMIT 1), 'BSCE', 'Bachelor of Science in Civil Engineering', 'Structural Engineering', 'UNDERGRADUATE', 'CMO No. 92 s. 2017', 'GP-BSCV-2020-006', 166, TRUE),
    (16, (SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), (SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_fm' LIMIT 1), 'BSBA-FM', 'Bachelor of Science in Business Administration', 'Financial Management', 'UNDERGRADUATE', 'CMO No. 17 s. 2017', 'GP-BSBA-2020-007', 138, TRUE),
    (17, (SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), (SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_fm' LIMIT 1), 'BSA', 'Bachelor of Science in Accountancy', 'Auditing and Financial Accounting', 'UNDERGRADUATE', 'CMO No. 27 s. 2017', 'GP-BSA-2020-008', 168, TRUE),
    (18, (SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), (SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_fm' LIMIT 1), 'BSHM', 'Bachelor of Science in Hospitality Management', 'Hospitality Operations', 'UNDERGRADUATE', 'CMO No. 62 s. 2017', 'GP-BSHM-2020-009', 140, TRUE),
    (19, (SELECT id FROM departments WHERE code = 'COED' LIMIT 1), (SELECT id FROM departments WHERE code = 'COED' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_seced' LIMIT 1), 'BSED-MATH', 'Bachelor of Secondary Education', 'Mathematics Education', 'UNDERGRADUATE', 'CMO No. 75 s. 2017', 'GP-BSED-2020-010', 142, TRUE),
    (20, (SELECT id FROM departments WHERE code = 'COED' LIMIT 1), (SELECT id FROM departments WHERE code = 'COED' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_seced' LIMIT 1), 'BSED-ENG', 'Bachelor of Secondary Education', 'English Language Education', 'UNDERGRADUATE', 'CMO No. 75 s. 2017', 'GP-BSED-2020-011', 142, TRUE),
    (21, (SELECT id FROM departments WHERE code = 'COED' LIMIT 1), (SELECT id FROM departments WHERE code = 'COED' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_seced' LIMIT 1), 'BEED', 'Bachelor of Elementary Education', 'Generalist', 'UNDERGRADUATE', 'CMO No. 74 s. 2017', 'GP-BEED-2020-012', 139, TRUE),
    (22, (SELECT id FROM departments WHERE code = 'CCJ' LIMIT 1), (SELECT id FROM departments WHERE code = 'CCJ' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_crim' LIMIT 1), 'BSCRIM', 'Bachelor of Science in Criminology', 'Law Enforcement & Forensics', 'UNDERGRADUATE', 'CMO No. 05 s. 2018', 'GP-BCRM-2020-013', 165, TRUE)
AS new_prog ON DUPLICATE KEY UPDATE name = new_prog.name, total_units_required = new_prog.total_units_required;

-- Dynamic Academic Years & Terms
INSERT INTO academic_years (id, code, start_date, end_date, is_current)
VALUES 
    (10, 'AY-2026-2027', DATE_SUB(CURRENT_DATE, INTERVAL 45 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 320 DAY), TRUE),
    (11, 'AY-2025-2026', DATE_SUB(CURRENT_DATE, INTERVAL 410 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 46 DAY), FALSE)
AS new_ay ON DUPLICATE KEY UPDATE 
    start_date = new_ay.start_date, 
    end_date = new_ay.end_date,
    is_current = new_ay.is_current;

UPDATE academic_years SET is_current = FALSE WHERE id <> 10;

INSERT INTO terms (id, academic_year_id, term_type, start_date, end_date, enrollment_open, grading_open, add_drop_open, is_active, max_hours_per_class)
VALUES 
    (10, (SELECT id FROM academic_years WHERE code = 'AY-2026-2027' LIMIT 1), 'FIRST_SEM', DATE_SUB(CURRENT_DATE, INTERVAL 45 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 105 DAY), TRUE, TRUE, TRUE, TRUE, 5.0),
    (11, (SELECT id FROM academic_years WHERE code = 'AY-2026-2027' LIMIT 1), 'SECOND_SEM', DATE_ADD(CURRENT_DATE, INTERVAL 110 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 260 DAY), FALSE, FALSE, FALSE, FALSE, 5.0),
    (12, (SELECT id FROM academic_years WHERE code = 'AY-2025-2026' LIMIT 1), 'FIRST_SEM', DATE_SUB(CURRENT_DATE, INTERVAL 400 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 250 DAY), FALSE, FALSE, FALSE, FALSE, 5.0),
    (13, (SELECT id FROM academic_years WHERE code = 'AY-2025-2026' LIMIT 1), 'SECOND_SEM', DATE_SUB(CURRENT_DATE, INTERVAL 240 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY), FALSE, FALSE, FALSE, FALSE, 5.0)
AS new_term ON DUPLICATE KEY UPDATE 
    start_date = new_term.start_date,
    end_date = new_term.end_date,
    enrollment_open = new_term.enrollment_open, 
    is_active = new_term.is_active;

UPDATE terms SET is_active = FALSE WHERE id <> 10;

-- Physical Learning Spaces & Specialized Laboratories
INSERT INTO rooms (id, campus_id, code, name, building, floor, capacity, room_type, is_active)
VALUES 
    (10, (SELECT id FROM campuses WHERE code = 'TALISAY_MAIN' LIMIT 1), 'CLAB-1', 'Computer Laboratory 1', 'CCS Building', 2, 40, 'LABORATORY', TRUE),
    (11, (SELECT id FROM campuses WHERE code = 'TALISAY_MAIN' LIMIT 1), 'CLAB-2', 'Computer Laboratory 2', 'CCS Building', 2, 40, 'LABORATORY', TRUE),
    (12, (SELECT id FROM campuses WHERE code = 'TALISAY_MAIN' LIMIT 1), 'ENG-LAB1', 'Electronics & Circuits Lab', 'COE Building', 1, 35, 'LABORATORY', TRUE),
    (13, (SELECT id FROM campuses WHERE code = 'TALISAY_MAIN' LIMIT 1), 'ENG-LAB2', 'Materials & Testing Lab', 'COE Building', 1, 30, 'LABORATORY', TRUE),
    (14, (SELECT id FROM campuses WHERE code = 'TALISAY_MAIN' LIMIT 1), 'LEC-201', 'General Lecture Hall 201', 'Academic Hall A', 2, 50, 'LECTURE', TRUE),
    (15, (SELECT id FROM campuses WHERE code = 'TALISAY_MAIN' LIMIT 1), 'LEC-202', 'General Lecture Hall 202', 'Academic Hall A', 2, 50, 'LECTURE', TRUE),
    (16, (SELECT id FROM campuses WHERE code = 'FORTUNE_TOWNE' LIMIT 1), 'CBMA-AUD', 'Business Multimedia Amphitheater', 'Business Complex', 3, 80, 'AUDITORIUM', TRUE),
    (17, (SELECT id FROM campuses WHERE code = 'BINALBAGAN' LIMIT 1), 'CRIM-MOOT', 'Moot Court & Forensics Lab', 'Criminology Hall', 1, 45, 'LABORATORY', TRUE),
    (18, (SELECT id FROM campuses WHERE code = 'FORTUNE_TOWNE' LIMIT 1), 'HM-KITCHEN', 'Commercial Culinary Kitchen Lab', 'Hospitality Wing', 1, 30, 'LABORATORY', TRUE)
AS new_room ON DUPLICATE KEY UPDATE name = new_room.name, capacity = new_room.capacity;

-- -----------------------------------------------------------------------------
-- 3. CHED CMO COURSE CATALOG, GENERAL EDUCATION & PREREQUISITES (DAG)
-- -----------------------------------------------------------------------------
INSERT INTO courses (id, code, title, lecture_units, lab_units, credit_units, contact_hours_lec, contact_hours_lab, category, description, is_active)
VALUES 
    -- CHED CMO No. 20, Series of 2013: General Education Core (36 units)
    (100, 'GEC101', 'Purposive Communication', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Writing, speaking, and presenting to different audiences and for various purposes.', TRUE),
    (101, 'GEC102', 'Understanding the Self', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Nature of identity, factors and forces that affect the development of personal identity.', TRUE),
    (102, 'GEC103', 'Readings in Philippine History', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Philippine history viewed from the lens of selected primary sources in different periods.', TRUE),
    (103, 'GEC104', 'The Contemporary World', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Globalization and its impact on individuals, communities, and nations.', TRUE),
    (104, 'GEC105', 'Mathematics in the Modern World', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Nature of mathematics, appreciation of practical, intellectual, and aesthetic dimensions.', TRUE),
    (105, 'GEC106', 'Art Appreciation', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Appreciation, analysis, and critique of various forms of visual, audio, and performing arts.', TRUE),
    (106, 'GEC107', 'Science, Technology, and Society', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Interactions between science and technology and social, cultural, political, and economic contexts.', TRUE),
    (107, 'GEC108', 'Ethics', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Principles of ethical behavior in modern society at the level of the person, society, and nature.', TRUE),
    (108, 'GEC-RIZAL', 'Life and Works of Rizal', 3.00, 0.00, 3.00, 3, 0, 'MANDATED_COURSE', 'Mandated by RA 1425: Study of the life, works, and writings of Jose Rizal.', TRUE),
    (109, 'PE101', 'Movement Competency (PATHFit 1)', 2.00, 0.00, 2.00, 2, 0, 'PHYSICAL_EDUCATION', 'Fundamental movement patterns and core engagement exercises.', TRUE),
    (110, 'PE102', 'Fitness-Based Activities (PATHFit 2)', 2.00, 0.00, 2.00, 2, 0, 'PHYSICAL_EDUCATION', 'Cardiovascular endurance, muscular strength, and circuit conditioning.', TRUE),
    (111, 'NSTP101', 'National Service Training Program 1', 3.00, 0.00, 3.00, 3, 0, 'MANDATED_COURSE', 'Mandated by RA 9163: Civic Welfare Training Service and literacy training.', TRUE),
    (112, 'NSTP102', 'National Service Training Program 2', 3.00, 0.00, 3.00, 3, 0, 'MANDATED_COURSE', 'Mandated by RA 9163: Community immersion and project implementation.', TRUE),

    -- Computing Common Core Courses (CMO 25 s. 2015)
    (10, 'CS101', 'Introduction to Computing', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'Computing principles, computer hardware architecture, binary logic, and problem analysis.', TRUE),
    (11, 'CS102', 'Fundamentals of Programming', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'Control structures, arrays, pointers, functions, and structured modular programming.', TRUE),
    (12, 'CS201', 'Intermediate Programming', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'Object-oriented programming, classes, encapsulation, inheritance, polymorphism, and exceptions.', TRUE),
    (13, 'CS301', 'Data Structures and Algorithms', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'Stacks, queues, linked lists, balanced binary trees, graphs, sorting, and Big-O notation.', TRUE),
    (120, 'CC105', 'Information Management & Databases', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'Relational data modeling, ACID transactions, ER diagrams, 3NF normalization, and complex SQL.', TRUE),
    (121, 'CC106', 'Applications Development & Emerging Tech', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'Modern cloud applications, responsive UI frameworks, REST web services, and microservices.', TRUE),

    -- Computer Science Professional Majors (CMO 25 s. 2015)
    (122, 'CS202', 'Discrete Structures', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Set theory, predicate logic, relations, recurrence relations, and graph theory proofs.', TRUE),
    (123, 'CS203', 'Design and Analysis of Algorithms', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Divide-and-conquer, greedy techniques, dynamic programming, NP-completeness, and heuristics.', TRUE),
    (124, 'CS204', 'Computer Architecture & Organization', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Instruction set architectures, CPU pipeline design, cache hierarchy, and assembly programming.', TRUE),
    (125, 'CS302', 'Software Engineering', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Agile methodologies, UML modeling, architectural design patterns, CI/CD, and quality assurance.', TRUE),
    (126, 'CS303', 'Automata Theory & Formal Languages', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'DFA, NFA, regular grammars, pushdown automata, context-free languages, and Turing machines.', TRUE),
    (127, 'CS304', 'Operating Systems', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Process concurrency, mutual exclusion, CPU scheduling, virtual memory, and file systems.', TRUE),
    (128, 'CS305', 'Intelligent Systems & Machine Learning', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Supervised learning, deep neural networks, clustering, reinforcement learning, and LLMs.', TRUE),
    (129, 'CS401', 'CS Capstone Project 1', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Research proposal, requirements engineering, methodology design, and defense.', TRUE),
    (130, 'CS402', 'CS Capstone Project 2', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Full system deployment, empirical validation, user testing, and public capstone colloquium.', TRUE),

    -- Information Technology Professional Majors (CMO 25 s. 2015)
    (131, 'IT201', 'Platform Technologies', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Linux administration, shell scripting, virtualization, containerization, and server setup.', TRUE),
    (132, 'IT202', 'Web Systems and Technologies', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Client-side reactive frameworks, HTTP protocol, REST APIs, and state management.', TRUE),
    (133, 'IT203', 'Networking 1 - Fundamentals', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'OSI model, TCP/IP protocol suite, subnetting, IPv4/IPv6, switching, and routing protocols.', TRUE),
    (134, 'IT301', 'Information Assurance & Security 1', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Cryptographic primitives, PKI, ethical hacking, OWASP Top 10 vulnerabilities, and firewalls.', TRUE),
    (135, 'IT302', 'Systems Integration and Architecture', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Enterprise integration patterns, messaging queues, ETL pipelines, and API gateways.', TRUE),
    (136, 'IT401', 'IT Capstone Project 1', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Enterprise solution architecture and capstone project proposal defense.', TRUE),

    -- Engineering Professional Courses (CMO 87, 92, 101 s. 2017)
    (140, 'ENGR101', 'Differential Calculus', 3.00, 0.00, 3.00, 3, 0, 'ENGINEERING_MATH', 'Limits, rates of change, implicit differentiation, and optimization problems.', TRUE),
    (141, 'ENGR102', 'Integral Calculus', 3.00, 0.00, 3.00, 3, 0, 'ENGINEERING_MATH', 'Definite and indefinite integrals, integration techniques, areas, and volumes of revolution.', TRUE),
    (142, 'ENGR201', 'Differential Equations', 3.00, 0.00, 3.00, 3, 0, 'ENGINEERING_MATH', 'First-order and higher-order ODEs, Laplace transforms, and series solutions.', TRUE),
    (143, 'ENGR202', 'Physics for Engineers', 3.00, 1.00, 4.00, 3, 3, 'ENGINEERING_SCIENCE', 'Classical mechanics, thermodynamics, electromagnetism, and wave motion with laboratory.', TRUE),
    (144, 'CPE201', 'Logic Circuits and Design', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Boolean algebra, Karnaugh maps, combinational logic, flip-flops, and FPGA synthesis.', TRUE),
    (145, 'CPE301', 'Embedded Systems & IoT', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Microcontrollers (ARM Cortex/ESP32), RTOS, sensor interfacing, and MQTT communication.', TRUE),
    (146, 'CE201', 'Statics of Rigid Bodies', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Force vectors, equilibrium of particles, trusses, centroids, and moments of inertia.', TRUE),
    (147, 'CE301', 'Structural Theory 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Determinate structures, influence lines for beams and trusses, and deflection computations.', TRUE),

    -- Business & Accountancy Courses (CMO 17, 27 s. 2017)
    (150, 'BM101', 'Principles of Management & Organization', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Management functions: planning, organizing, leading, and controlling in modern corporations.', TRUE),
    (151, 'BM102', 'Marketing Management', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Market research, consumer behavior, product design, pricing strategies, and branding.', TRUE),
    (152, 'ACT101', 'Financial Accounting & Reporting 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Accounting cycle, financial statements preparation, and journal voucher entries under PFRS.', TRUE),
    (153, 'ACT102', 'Conceptual Framework & Acctg Standards', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'IASB conceptual framework, recognition, measurement, and presentation of assets.', TRUE),
    (154, 'ACT201', 'Intermediate Accounting 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Accounting for cash, receivables, inventories, and property, plant, and equipment.', TRUE),
    (155, 'FM201', 'Financial Management & Capital Markets', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Time value of money, working capital management, risk-return trade-offs, and securities.', TRUE),
    (156, 'HM101', 'Introduction to Hospitality Management', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Overview of the lodging, food service, tourism, and recreation management industries.', TRUE),

    -- Teacher Education Courses (CMO 74, 75 s. 2017)
    (160, 'ED101', 'The Child and Adolescent Learners', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_EDUCATION', 'Psychological theories, biological maturation, socio-cultural influences, and stages of child growth.', TRUE),
    (161, 'ED102', 'Facilitating Learner-Centered Teaching', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_EDUCATION', 'Cognitive learning styles, motivational factors, differentiated instruction, and active pedagogy.', TRUE),
    (162, 'ED201', 'Assessment in Learning 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_EDUCATION', 'Traditional and authentic assessment, test construction, item analysis, and psychometrics.', TRUE),
    (163, 'EDM201', 'College & Advanced Algebra for Teachers', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Equations, inequalities, polynomial functions, and algebraic modeling for educators.', TRUE),
    (164, 'EDE201', 'Structure of English', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Morphology, syntax, phonology, and generative transformational grammar for English educators.', TRUE),

    -- Criminology Courses (CMO 05 s. 2018)
    (170, 'CRIM101', 'Intro to Philippine Criminal Justice System', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'The 5 pillars: Law Enforcement, Prosecution, Judiciary, Penology, and Community.', TRUE),
    (171, 'CRIM102', 'Theories of Crime Causation', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Classical, biological, psychological, and sociological explanations of criminal behavior.', TRUE),
    (172, 'CRIM201', 'Criminal Law (Book 1 - Revised Penal Code)', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Felonies, criminal liability, justifying, exempting, mitigating, and aggravating circumstances.', TRUE),
    (173, 'CRIM202', 'Forensic Chemistry & Toxicology', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Chemical examination of physical evidence, narcotics, firearms discharge, and toxic agents.', TRUE)
AS new_course ON DUPLICATE KEY UPDATE 
    title = new_course.title, 
    credit_units = new_course.credit_units,
    category = new_course.category;

-- Curricula for Degree Programs (Status: ACTIVE for live operations)
INSERT INTO curricula (id, program_id, code, name, status, version_number, effective_academic_year, is_active)
VALUES 
    (10, (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), 'BSCS-2026', 'BSCS CMO 25 s. 2015 Approved Outcome-Based Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    (11, (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'BSIT-2026', 'BSIT CMO 25 s. 2015 Approved Outcome-Based Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    (12, (SELECT id FROM programs WHERE code = 'BSIS' LIMIT 1), 'BSIS-2026', 'BSIS CMO 25 s. 2015 Approved Outcome-Based Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    (13, (SELECT id FROM programs WHERE code = 'BSCpE' LIMIT 1), 'BSCpE-2026', 'BSCpE CMO 87 s. 2017 Approved Engineering Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    (14, (SELECT id FROM programs WHERE code = 'BSECE' LIMIT 1), 'BSECE-2026', 'BSECE CMO 101 s. 2017 Approved Telecomm Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    (15, (SELECT id FROM programs WHERE code = 'BSCE' LIMIT 1), 'BSCE-2026', 'BSCE CMO 92 s. 2017 Approved Structural Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    (16, (SELECT id FROM programs WHERE code = 'BSBA-FM' LIMIT 1), 'BSBA-FM-2026', 'BSBA-FM CMO 17 s. 2017 Financial Management Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    (17, (SELECT id FROM programs WHERE code = 'BSA' LIMIT 1), 'BSA-2026', 'BSA CMO 27 s. 2017 Professional Accountancy Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    (18, (SELECT id FROM programs WHERE code = 'BSHM' LIMIT 1), 'BSHM-2026', 'BSHM CMO 62 s. 2017 Hospitality Operations Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    (19, (SELECT id FROM programs WHERE code = 'BSED-MATH' LIMIT 1), 'BSED-MATH-2026', 'BSED-MATH CMO 75 s. 2017 Secondary Mathematics Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    (20, (SELECT id FROM programs WHERE code = 'BSED-ENG' LIMIT 1), 'BSED-ENG-2026', 'BSED-ENG CMO 75 s. 2017 Secondary English Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    (21, (SELECT id FROM programs WHERE code = 'BEED' LIMIT 1), 'BEED-2026', 'BEED CMO 74 s. 2017 Elementary Education Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    (22, (SELECT id FROM programs WHERE code = 'BSCRIM' LIMIT 1), 'BSCRIM-2026', 'BSCRIM CMO 05 s. 2018 Criminology Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE)
AS new_curr ON DUPLICATE KEY UPDATE status = new_curr.status, is_active = new_curr.is_active;

-- Map Courses to Curricula
INSERT INTO curriculum_courses (id, curriculum_id, course_id, year_level, sequence_order, category, semester)
VALUES 
    -- BSCS Curriculum Mapping
    (10, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GEC101' LIMIT 1), 1, 1, 'GENERAL_EDUCATION', 'FIRST_SEMESTER'),
    (11, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GEC105' LIMIT 1), 1, 2, 'GENERAL_EDUCATION', 'FIRST_SEMESTER'),
    (12, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS101' LIMIT 1),  1, 3, 'INSTITUTIONAL_CORE', 'FIRST_SEMESTER'),
    (13, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS102' LIMIT 1),  1, 4, 'INSTITUTIONAL_CORE', 'FIRST_SEMESTER'),
    (14, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'PE101' LIMIT 1), 1, 5, 'PHYSICAL_EDUCATION', 'FIRST_SEMESTER'),
    (15, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'NSTP101' LIMIT 1), 1, 6, 'MANDATED_COURSE', 'FIRST_SEMESTER'),
    (16, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GEC102' LIMIT 1), 1, 1, 'GENERAL_EDUCATION', 'SECOND_SEMESTER'),
    (17, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS201' LIMIT 1),  1, 2, 'INSTITUTIONAL_CORE', 'SECOND_SEMESTER'),
    (18, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS202' LIMIT 1), 1, 3, 'PROFESSIONAL_MAJOR', 'SECOND_SEMESTER'),
    (19, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'PE102' LIMIT 1), 1, 4, 'PHYSICAL_EDUCATION', 'SECOND_SEMESTER'),
    (20, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'NSTP102' LIMIT 1), 1, 5, 'MANDATED_COURSE', 'SECOND_SEMESTER'),
    (21, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS301' LIMIT 1),  2, 1, 'INSTITUTIONAL_CORE', 'FIRST_SEMESTER'),
    (22, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CC105' LIMIT 1), 2, 2, 'INSTITUTIONAL_CORE', 'FIRST_SEMESTER'),
    (23, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS204' LIMIT 1), 2, 3, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (24, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CC106' LIMIT 1), 2, 1, 'INSTITUTIONAL_CORE', 'SECOND_SEMESTER'),
    (25, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS203' LIMIT 1), 2, 2, 'PROFESSIONAL_MAJOR', 'SECOND_SEMESTER'),
    (26, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS302' LIMIT 1), 3, 1, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (27, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS303' LIMIT 1), 3, 2, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (28, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS304' LIMIT 1), 3, 1, 'PROFESSIONAL_MAJOR', 'SECOND_SEMESTER'),
    (29, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS305' LIMIT 1), 3, 2, 'PROFESSIONAL_MAJOR', 'SECOND_SEMESTER'),
    (30, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS401' LIMIT 1), 4, 1, 'CAPSTONE', 'FIRST_SEMESTER'),
    (31, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GEC-RIZAL' LIMIT 1), 4, 2, 'MANDATED_COURSE', 'FIRST_SEMESTER'),
    (32, (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS402' LIMIT 1), 4, 1, 'CAPSTONE', 'SECOND_SEMESTER'),

    -- BSIT Curriculum Mapping
    (40, (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GEC101' LIMIT 1), 1, 1, 'GENERAL_EDUCATION', 'FIRST_SEMESTER'),
    (41, (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS101' LIMIT 1),  1, 2, 'INSTITUTIONAL_CORE', 'FIRST_SEMESTER'),
    (42, (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS102' LIMIT 1),  1, 3, 'INSTITUTIONAL_CORE', 'FIRST_SEMESTER'),
    (43, (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS201' LIMIT 1),  1, 1, 'INSTITUTIONAL_CORE', 'SECOND_SEMESTER'),
    (44, (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT201' LIMIT 1), 2, 1, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (45, (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT202' LIMIT 1), 2, 2, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (46, (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT203' LIMIT 1), 2, 3, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (47, (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT301' LIMIT 1), 3, 1, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (48, (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT302' LIMIT 1), 3, 2, 'PROFESSIONAL_MAJOR', 'SECOND_SEMESTER'),
    (49, (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT401' LIMIT 1), 4, 1, 'CAPSTONE', 'FIRST_SEMESTER'),

    -- Engineering (BSCpE, BSCE) Curriculum Mapping
    (50, (SELECT id FROM curricula WHERE code = 'BSCpE-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'ENGR101' LIMIT 1), 1, 1, 'ENGINEERING_MATH', 'FIRST_SEMESTER'),
    (51, (SELECT id FROM curricula WHERE code = 'BSCpE-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS101' LIMIT 1),   1, 2, 'INSTITUTIONAL_CORE', 'FIRST_SEMESTER'),
    (52, (SELECT id FROM curricula WHERE code = 'BSCpE-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'ENGR102' LIMIT 1), 1, 1, 'ENGINEERING_MATH', 'SECOND_SEMESTER'),
    (53, (SELECT id FROM curricula WHERE code = 'BSCpE-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS102' LIMIT 1),   1, 2, 'INSTITUTIONAL_CORE', 'SECOND_SEMESTER'),
    (54, (SELECT id FROM curricula WHERE code = 'BSCpE-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CPE201' LIMIT 1),  2, 1, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (55, (SELECT id FROM curricula WHERE code = 'BSCpE-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'ENGR202' LIMIT 1), 2, 2, 'ENGINEERING_SCIENCE', 'FIRST_SEMESTER'),
    (56, (SELECT id FROM curricula WHERE code = 'BSCpE-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CPE301' LIMIT 1),  3, 1, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (57, (SELECT id FROM curricula WHERE code = 'BSCE-2026' LIMIT 1),  (SELECT id FROM courses WHERE code = 'ENGR101' LIMIT 1), 1, 1, 'ENGINEERING_MATH', 'FIRST_SEMESTER'),
    (58, (SELECT id FROM curricula WHERE code = 'BSCE-2026' LIMIT 1),  (SELECT id FROM courses WHERE code = 'ENGR102' LIMIT 1), 1, 1, 'ENGINEERING_MATH', 'SECOND_SEMESTER'),
    (59, (SELECT id FROM curricula WHERE code = 'BSCE-2026' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CE201' LIMIT 1),   2, 1, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (60, (SELECT id FROM curricula WHERE code = 'BSCE-2026' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CE301' LIMIT 1),   3, 1, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),

    -- Business (BSBA-FM, BSA) Curriculum Mapping
    (70, (SELECT id FROM curricula WHERE code = 'BSBA-FM-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GEC101' LIMIT 1), 1, 1, 'GENERAL_EDUCATION', 'FIRST_SEMESTER'),
    (71, (SELECT id FROM curricula WHERE code = 'BSBA-FM-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'BM101' LIMIT 1),  1, 2, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (72, (SELECT id FROM curricula WHERE code = 'BSBA-FM-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'BM102' LIMIT 1),  1, 1, 'PROFESSIONAL_MAJOR', 'SECOND_SEMESTER'),
    (73, (SELECT id FROM curricula WHERE code = 'BSBA-FM-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'ACT101' LIMIT 1), 2, 1, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (74, (SELECT id FROM curricula WHERE code = 'BSBA-FM-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'FM201' LIMIT 1),  2, 2, 'PROFESSIONAL_MAJOR', 'SECOND_SEMESTER'),
    (75, (SELECT id FROM curricula WHERE code = 'BSA-2026' LIMIT 1),     (SELECT id FROM courses WHERE code = 'ACT101' LIMIT 1), 1, 1, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (76, (SELECT id FROM curricula WHERE code = 'BSA-2026' LIMIT 1),     (SELECT id FROM courses WHERE code = 'ACT102' LIMIT 1), 1, 2, 'PROFESSIONAL_MAJOR', 'SECOND_SEMESTER'),
    (77, (SELECT id FROM curricula WHERE code = 'BSA-2026' LIMIT 1),     (SELECT id FROM courses WHERE code = 'ACT201' LIMIT 1), 2, 1, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),

    -- Teacher Education & Criminology Curriculum Mapping
    (80, (SELECT id FROM curricula WHERE code = 'BSED-MATH-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'ED101' LIMIT 1),  1, 1, 'PROFESSIONAL_EDUCATION', 'FIRST_SEMESTER'),
    (81, (SELECT id FROM curricula WHERE code = 'BSED-MATH-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'ED102' LIMIT 1),  1, 2, 'PROFESSIONAL_EDUCATION', 'SECOND_SEMESTER'),
    (82, (SELECT id FROM curricula WHERE code = 'BSED-MATH-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'ED201' LIMIT 1),  2, 1, 'PROFESSIONAL_EDUCATION', 'FIRST_SEMESTER'),
    (83, (SELECT id FROM curricula WHERE code = 'BSED-MATH-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'EDM201' LIMIT 1), 2, 2, 'SPECIALIZATION_MAJOR', 'SECOND_SEMESTER'),
    (90, (SELECT id FROM curricula WHERE code = 'BSCRIM-2026' LIMIT 1),    (SELECT id FROM courses WHERE code = 'CRIM101' LIMIT 1), 1, 1, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (91, (SELECT id FROM curricula WHERE code = 'BSCRIM-2026' LIMIT 1),    (SELECT id FROM courses WHERE code = 'CRIM102' LIMIT 1), 1, 2, 'PROFESSIONAL_MAJOR', 'SECOND_SEMESTER'),
    (92, (SELECT id FROM curricula WHERE code = 'BSCRIM-2026' LIMIT 1),    (SELECT id FROM courses WHERE code = 'CRIM201' LIMIT 1), 2, 1, 'PROFESSIONAL_MAJOR', 'FIRST_SEMESTER'),
    (93, (SELECT id FROM curricula WHERE code = 'BSCRIM-2026' LIMIT 1),    (SELECT id FROM courses WHERE code = 'CRIM202' LIMIT 1), 2, 2, 'PROFESSIONAL_MAJOR', 'SECOND_SEMESTER')
AS new_curr_course ON DUPLICATE KEY UPDATE year_level = new_curr_course.year_level, semester = new_curr_course.semester;

-- Prerequisite Chains: Strictly Enforcing Directed Acyclic Graph (DAG) Invariant
INSERT INTO course_prerequisites (id, course_id, prerequisite_course_id, rule_type, min_grade_required)
VALUES 
    -- Computing Core Progression
    (10, (SELECT id FROM courses WHERE code = 'CS102' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CS101' LIMIT 1),  'HARD', '3.00'),
    (11, (SELECT id FROM courses WHERE code = 'CS201' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CS102' LIMIT 1),  'HARD', '3.00'),
    (12, (SELECT id FROM courses WHERE code = 'CS301' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CS201' LIMIT 1),  'HARD', '3.00'),
    (13, (SELECT id FROM courses WHERE code = 'CC105' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CS102' LIMIT 1),  'HARD', '3.00'),
    (14, (SELECT id FROM courses WHERE code = 'CC106' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CS301' LIMIT 1),  'HARD', '3.00'),
    (15, (SELECT id FROM courses WHERE code = 'CS203' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CS301' LIMIT 1),  'HARD', '3.00'),
    (16, (SELECT id FROM courses WHERE code = 'CS203' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CS202' LIMIT 1),  'HARD', '3.00'),
    (17, (SELECT id FROM courses WHERE code = 'CS302' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CS201' LIMIT 1),  'HARD', '3.00'),
    (18, (SELECT id FROM courses WHERE code = 'CS303' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CS203' LIMIT 1),  'HARD', '3.00'),
    (19, (SELECT id FROM courses WHERE code = 'CS304' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CS204' LIMIT 1),  'HARD', '3.00'),
    (20, (SELECT id FROM courses WHERE code = 'CS305' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CS203' LIMIT 1),  'HARD', '3.00'),
    (21, (SELECT id FROM courses WHERE code = 'CS401' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CS302' LIMIT 1),  'HARD', '3.00'),
    (22, (SELECT id FROM courses WHERE code = 'CS402' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CS401' LIMIT 1),  'HARD', '3.00'),

    -- Engineering Mathematics & Applied Progression
    (30, (SELECT id FROM courses WHERE code = 'ENGR102' LIMIT 1), (SELECT id FROM courses WHERE code = 'ENGR101' LIMIT 1), 'HARD', '3.00'),
    (31, (SELECT id FROM courses WHERE code = 'ENGR201' LIMIT 1), (SELECT id FROM courses WHERE code = 'ENGR102' LIMIT 1), 'HARD', '3.00'),
    (32, (SELECT id FROM courses WHERE code = 'CPE201' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CS102' LIMIT 1),   'HARD', '3.00'),
    (33, (SELECT id FROM courses WHERE code = 'CPE301' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CPE201' LIMIT 1),  'HARD', '3.00'),
    (34, (SELECT id FROM courses WHERE code = 'CE201' LIMIT 1),   (SELECT id FROM courses WHERE code = 'ENGR101' LIMIT 1), 'HARD', '3.00'),
    (35, (SELECT id FROM courses WHERE code = 'CE301' LIMIT 1),   (SELECT id FROM courses WHERE code = 'CE201' LIMIT 1),   'HARD', '3.00'),

    -- Business & Accountancy Progression
    (40, (SELECT id FROM courses WHERE code = 'ACT102' LIMIT 1), (SELECT id FROM courses WHERE code = 'ACT101' LIMIT 1), 'HARD', '3.00'),
    (41, (SELECT id FROM courses WHERE code = 'ACT201' LIMIT 1), (SELECT id FROM courses WHERE code = 'ACT102' LIMIT 1), 'HARD', '3.00'),
    (42, (SELECT id FROM courses WHERE code = 'FM201' LIMIT 1),  (SELECT id FROM courses WHERE code = 'ACT101' LIMIT 1), 'HARD', '3.00'),

    -- Education & Criminology Progression
    (50, (SELECT id FROM courses WHERE code = 'ED102' LIMIT 1),   (SELECT id FROM courses WHERE code = 'ED101' LIMIT 1),   'HARD', '3.00'),
    (51, (SELECT id FROM courses WHERE code = 'ED201' LIMIT 1),   (SELECT id FROM courses WHERE code = 'ED102' LIMIT 1),   'HARD', '3.00'),
    (52, (SELECT id FROM courses WHERE code = 'CRIM102' LIMIT 1), (SELECT id FROM courses WHERE code = 'CRIM101' LIMIT 1), 'HARD', '3.00'),
    (53, (SELECT id FROM courses WHERE code = 'CRIM201' LIMIT 1), (SELECT id FROM courses WHERE code = 'CRIM101' LIMIT 1), 'HARD', '3.00')
AS new_prereq ON DUPLICATE KEY UPDATE min_grade_required = new_prereq.min_grade_required;

-- -----------------------------------------------------------------------------
-- 4. OUTCOMES-BASED EDUCATION (OBE) HIERARCHY (IILO -> PILO -> CILO)
-- -----------------------------------------------------------------------------
INSERT INTO institutional_outcomes (id, code, statement, description, active)
VALUES 
    (10, 'IILO-01', 'Demonstrate professional competence and lifelong learning in their chosen specialization.', 'Graduates demonstrate technical proficiency, critical inquiry, and commitment to continuous development.', TRUE),
    (11, 'IILO-02', 'Exhibit critical thinking, analytical reasoning, and innovative problem-solving.', 'Graduates apply rigorous multidisciplinary methods to design viable solutions for societal challenges.', TRUE),
    (12, 'IILO-03', 'Communicate effectively and lead collaboratively across diverse teams.', 'Graduates convey complex ideas articulately across technical, corporate, and public spheres.', TRUE),
    (13, 'IILO-04', 'Act with ethical integrity, social responsibility, and commitment to sustainable community development.', 'Graduates uphold the highest ethical standards, environmental stewardship, and community responsiveness.', TRUE)
AS new_iilo ON DUPLICATE KEY UPDATE statement = new_iilo.statement, active = new_iilo.active;

-- Program Outcomes (PILO)
INSERT INTO program_outcomes (id, program_id, code, description)
VALUES 
    -- BSCS PILOs
    (10, (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), 'PILO-CS-01', 'Apply knowledge of computing fundamentals, algorithmic principles, and theory to model complex systems.'),
    (11, (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), 'PILO-CS-02', 'Design, implement, and evaluate computer-based solutions meeting realistic operational constraints.'),
    (12, (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), 'PILO-CS-03', 'Analyze the local and global impact of advanced computing technologies on society, individuals, and organizations.'),
    (13, (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), 'PILO-CS-04', 'Function effectively on multidisciplinary teams to accomplish complex software engineering objectives.'),
    -- BSIT PILOs
    (14, (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'PILO-IT-01', 'Deploy, configure, and maintain secure enterprise IT infrastructure, operating systems, and networks.'),
    (15, (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'PILO-IT-02', 'Integrate user-centered design, modern database platforms, and scalable web architectures.'),
    -- BSCpE PILOs
    (16, (SELECT id FROM programs WHERE code = 'BSCpE' LIMIT 1), 'PILO-CPE-01', 'Design and validate digital hardware systems, microcontrollers, embedded IoT firmware, and robotics.'),
    -- BSBA PILOs
    (17, (SELECT id FROM programs WHERE code = 'BSBA-FM' LIMIT 1), 'PILO-BA-01', 'Formulate strategic financial decisions, capital budgeting models, and investment portfolio analyses.'),
    -- BSED PILOs
    (18, (SELECT id FROM programs WHERE code = 'BSED-MATH' LIMIT 1), 'PILO-ED-01', 'Demonstrate pedagogical mastery and content knowledge in secondary mathematics education.'),
    -- BSCRIM PILOs
    (19, (SELECT id FROM programs WHERE code = 'BSCRIM' LIMIT 1), 'PILO-CRIM-01', 'Apply constitutional rights, legal jurisprudence, and forensic investigative techniques in crime prevention.')
AS new_pilo ON DUPLICATE KEY UPDATE description = new_pilo.description;

-- PILO-IILO Mappings
INSERT INTO pilo_iilo_mappings (id, pilo_id, iilo_id)
VALUES 
    (10, (SELECT id FROM program_outcomes WHERE code = 'PILO-CS-01' LIMIT 1),   (SELECT id FROM institutional_outcomes WHERE code = 'IILO-01' LIMIT 1)),
    (11, (SELECT id FROM program_outcomes WHERE code = 'PILO-CS-02' LIMIT 1),   (SELECT id FROM institutional_outcomes WHERE code = 'IILO-02' LIMIT 1)),
    (12, (SELECT id FROM program_outcomes WHERE code = 'PILO-CS-03' LIMIT 1),   (SELECT id FROM institutional_outcomes WHERE code = 'IILO-04' LIMIT 1)),
    (13, (SELECT id FROM program_outcomes WHERE code = 'PILO-CS-04' LIMIT 1),   (SELECT id FROM institutional_outcomes WHERE code = 'IILO-03' LIMIT 1)),
    (14, (SELECT id FROM program_outcomes WHERE code = 'PILO-IT-01' LIMIT 1),   (SELECT id FROM institutional_outcomes WHERE code = 'IILO-01' LIMIT 1)),
    (15, (SELECT id FROM program_outcomes WHERE code = 'PILO-IT-02' LIMIT 1),   (SELECT id FROM institutional_outcomes WHERE code = 'IILO-02' LIMIT 1)),
    (16, (SELECT id FROM program_outcomes WHERE code = 'PILO-CPE-01' LIMIT 1),  (SELECT id FROM institutional_outcomes WHERE code = 'IILO-01' LIMIT 1)),
    (17, (SELECT id FROM program_outcomes WHERE code = 'PILO-BA-01' LIMIT 1),   (SELECT id FROM institutional_outcomes WHERE code = 'IILO-02' LIMIT 1)),
    (18, (SELECT id FROM program_outcomes WHERE code = 'PILO-ED-01' LIMIT 1),   (SELECT id FROM institutional_outcomes WHERE code = 'IILO-01' LIMIT 1)),
    (19, (SELECT id FROM program_outcomes WHERE code = 'PILO-CRIM-01' LIMIT 1), (SELECT id FROM institutional_outcomes WHERE code = 'IILO-04' LIMIT 1))
AS new_pim ON DUPLICATE KEY UPDATE pilo_id = new_pim.pilo_id;

-- Course Intended Learning Outcomes (CILO)
INSERT INTO course_outcomes (id, course_id, code, description, blooms_level)
VALUES 
    (10, (SELECT id FROM courses WHERE code = 'CS102' LIMIT 1), 'CILO-CS102-1', 'Construct modular computer programs applying fundamental control logic and procedural abstractions.', 'APPLY'),
    (11, (SELECT id FROM courses WHERE code = 'CS102' LIMIT 1), 'CILO-CS102-2', 'Debug, trace, and optimize source code implementations handling dynamic data arrays.', 'ANALYZE'),
    (12, (SELECT id FROM courses WHERE code = 'CS301' LIMIT 1), 'CILO-CS301-1', 'Analyze empirical time and space complexity of sorting, searching, and graph traversal algorithms.', 'ANALYZE'),
    (13, (SELECT id FROM courses WHERE code = 'CS301' LIMIT 1), 'CILO-CS301-2', 'Synthesize optimal linear and hierarchical non-linear data structures for real-world memory constraints.', 'CREATE'),
    (14, (SELECT id FROM courses WHERE code = 'CS302' LIMIT 1), 'CILO-CS302-1', 'Architect secure enterprise software systems following clean architecture and design patterns.', 'CREATE'),
    (15, (SELECT id FROM courses WHERE code = 'IT202' LIMIT 1), 'CILO-IT202-1', 'Develop responsive full-stack web applications consuming secured asynchronous RESTful endpoints.', 'APPLY')
AS new_cilo ON DUPLICATE KEY UPDATE description = new_cilo.description, blooms_level = new_cilo.blooms_level;

-- CILO-PILO Mappings
INSERT INTO cilo_pilo_mappings (id, course_outcome_id, program_outcome_id, mapping_type)
VALUES 
    (10, (SELECT id FROM course_outcomes WHERE code = 'CILO-CS102-1' LIMIT 1), (SELECT id FROM program_outcomes WHERE code = 'PILO-CS-01' LIMIT 1), 'I'),
    (11, (SELECT id FROM course_outcomes WHERE code = 'CILO-CS102-2' LIMIT 1), (SELECT id FROM program_outcomes WHERE code = 'PILO-CS-02' LIMIT 1), 'E'),
    (12, (SELECT id FROM course_outcomes WHERE code = 'CILO-CS301-1' LIMIT 1), (SELECT id FROM program_outcomes WHERE code = 'PILO-CS-01' LIMIT 1), 'E'),
    (13, (SELECT id FROM course_outcomes WHERE code = 'CILO-CS301-2' LIMIT 1), (SELECT id FROM program_outcomes WHERE code = 'PILO-CS-02' LIMIT 1), 'D'),
    (14, (SELECT id FROM course_outcomes WHERE code = 'CILO-CS302-1' LIMIT 1), (SELECT id FROM program_outcomes WHERE code = 'PILO-CS-02' LIMIT 1), 'D'),
    (15, (SELECT id FROM course_outcomes WHERE code = 'CILO-IT202-1' LIMIT 1), (SELECT id FROM program_outcomes WHERE code = 'PILO-IT-02' LIMIT 1), 'D')
AS new_cpm ON DUPLICATE KEY UPDATE mapping_type = new_cpm.mapping_type;

-- -----------------------------------------------------------------------------
-- 5. FACULTY PROFILES & WORKLOAD ASSIGNMENTS
-- -----------------------------------------------------------------------------
INSERT INTO faculty_profiles (id, user_id, college_id, program_id, faculty_id_number, highest_degree, academic_rank, prc_license_no, employment_status, is_tenured)
VALUES 
    (10, (SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1),      (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1),  (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1),  'FAC-2026-001', 'DOCTORATE', 'PROFESSOR_I', 'PRC-CS-9901', 'FULL_TIME', TRUE),
    (11, (SELECT id FROM users WHERE username = 'faculty_tan' LIMIT 1),        (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1),  (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1),  'FAC-2026-002', 'MASTERS', 'ASSOCIATE_PROFESSOR_I', 'PRC-CS-9902', 'FULL_TIME', TRUE),
    (12, (SELECT id FROM users WHERE username = 'faculty_garcia' LIMIT 1),     (SELECT id FROM departments WHERE code = 'COE' LIMIT 1),  (SELECT id FROM programs WHERE code = 'BSCpE' LIMIT 1), 'FAC-2026-003', 'MASTERS', 'ASSISTANT_PROFESSOR_I', 'PRC-ECE-55412', 'FULL_TIME', TRUE),
    (13, (SELECT id FROM users WHERE username = 'faculty_reyes' LIMIT 1),      (SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSA' LIMIT 1),   'FAC-2026-004', 'MASTERS', 'PROFESSOR_I', 'PRC-CPA-88912', 'FULL_TIME', TRUE),
    (14, (SELECT id FROM users WHERE username = 'faculty_delacruz' LIMIT 1),   (SELECT id FROM departments WHERE code = 'COED' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSED-MATH' LIMIT 1), 'FAC-2026-005', 'DOCTORATE', 'ASSOCIATE_PROFESSOR_I', 'PRC-LPT-11234', 'FULL_TIME', TRUE),
    (15, (SELECT id FROM users WHERE username = 'faculty_villanueva' LIMIT 1), (SELECT id FROM departments WHERE code = 'CCJ' LIMIT 1),  (SELECT id FROM programs WHERE code = 'BSCRIM' LIMIT 1), 'FAC-2026-006', 'MASTERS', 'ASSISTANT_PROFESSOR_I', 'PRC-CRIM-7781', 'FULL_TIME', TRUE),
    (16, (SELECT id FROM users WHERE username = 'faculty_aquino' LIMIT 1),     (SELECT id FROM departments WHERE code = 'CAS' LIMIT 1),  NULL, 'FAC-2026-007', 'MASTERS', 'INSTRUCTOR_I', 'PRC-LPT-99881', 'FULL_TIME', FALSE),
    (17, (SELECT id FROM users WHERE username = 'faculty_mendoza' LIMIT 1),    (SELECT id FROM departments WHERE code = 'CAS' LIMIT 1),  NULL, 'FAC-2026-008', 'BACHELORS', 'INSTRUCTOR_I', NULL, 'FULL_TIME', FALSE)
AS new_fac ON DUPLICATE KEY UPDATE academic_rank = new_fac.academic_rank, is_tenured = new_fac.is_tenured;

INSERT INTO faculty_workloads (id, term_id, faculty_user_id, regular_units, overload_units, total_contact_hours, is_overload_approved, approved_by_user_id, number_of_preparations)
VALUES 
    (10, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1),      18.00, 3.00, 21.00, TRUE,  (SELECT id FROM users WHERE username = 'dean_ccs' LIMIT 1), 2),
    (11, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM users WHERE username = 'faculty_tan' LIMIT 1),        18.00, 0.00, 18.00, FALSE, NULL, 2),
    (12, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM users WHERE username = 'faculty_garcia' LIMIT 1),     15.00, 0.00, 18.00, FALSE, NULL, 2),
    (13, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM users WHERE username = 'faculty_reyes' LIMIT 1),      18.00, 0.00, 18.00, FALSE, NULL, 2),
    (14, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM users WHERE username = 'faculty_delacruz' LIMIT 1),   18.00, 0.00, 18.00, FALSE, NULL, 2),
    (15, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM users WHERE username = 'faculty_villanueva' LIMIT 1), 18.00, 0.00, 18.00, FALSE, NULL, 2),
    (16, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM users WHERE username = 'faculty_aquino' LIMIT 1),     18.00, 0.00, 18.00, FALSE, NULL, 1),
    (17, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM users WHERE username = 'faculty_mendoza' LIMIT 1),    16.00, 0.00, 16.00, FALSE, NULL, 2)
AS new_fw ON DUPLICATE KEY UPDATE regular_units = new_fw.regular_units;

-- -----------------------------------------------------------------------------
-- 6. PUBLIC ADMISSION MANAGEMENT, ENTRANCE EXAM RESERVATION & QUEUE
-- Populated prior to student profiles to allow dynamic foreign key linkage
-- -----------------------------------------------------------------------------
INSERT INTO admission_configs (id, term_id, is_active, daily_slot_limit, total_opened_slots, days_open, start_date, end_date)
VALUES 
    (10, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), TRUE, 1000, 20000, 30, DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 45 DAY)),
    (11, (SELECT id FROM terms WHERE is_active = FALSE AND term_type = 'SECOND_SEM' LIMIT 1), FALSE, 1000, 20000, 20, DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 90 DAY))
AS new_acfg ON DUPLICATE KEY UPDATE 
    start_date = new_acfg.start_date,
    end_date = new_acfg.end_date,
    is_active = new_acfg.is_active;

INSERT INTO entrance_exam_slots (id, term_id, exam_date, start_time, end_time, venue_room, max_capacity, reserved_count, status)
VALUES 
    (10, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY),  '08:30:00', '11:30:00', 'University Amphitheater A', 100, 25, 'OPEN'),
    (11, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY),  '13:30:00', '16:30:00', 'University Amphitheater B', 100, 30, 'OPEN'),
    (12, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), '08:30:00', '11:30:00', 'Testing Center Hall 1',     80, 15, 'OPEN'),
    (13, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_ADD(CURRENT_DATE, INTERVAL 21 DAY), '08:30:00', '11:30:00', 'University Auditorium A',   100,  5, 'OPEN')
AS new_slot ON DUPLICATE KEY UPDATE 
    exam_date = new_slot.exam_date,
    status = new_slot.status;

INSERT INTO admission_applications (
    id, application_number, target_program_id, term_id, exam_slot_id, 
    first_name, middle_name, last_name, suffix, birth_date, birth_place, 
    gender, gender_identity, civil_status, citizenship, mobile_number, email, 
    lrn_number, high_school_name, deped_school_id, high_school_type, shs_track_and_strand, 
    high_school_gwa, shs_year_graduated, street_address, barangay, city_municipality, province, zip_code, 
    emergency_contact_name, emergency_contact_relationship, emergency_contact_number, 
    is_4ps_beneficiary, is_indigenous_people, is_person_with_disability, is_solo_parent, 
    is_gida_resident, is_farmer_fisherfolk, is_bottom_40_income_bracket, 
    monthly_household_income_bracket, is_first_generation_college, 
    application_status, is_enrolled, enrolled_at, exam_score, exam_remarks, interview_score, interview_remarks, 
    evaluated_by_user_id, interviewed_by_user_id
) VALUES 
    (10, 'ADM-2026-0001', (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM entrance_exam_slots WHERE venue_room = 'University Amphitheater A' LIMIT 1), 
     'Juan', 'Protasio', 'Dela Cruz', NULL, '2005-06-19', 'Talisay City', 
     'MALE', 'CISGENDER_MALE', 'SINGLE', 'FILIPINO', '+639171112233', 'juan.applicant@gmail.com', 
     '123456789012', 'Negros Occidental National High School', 'DEPED-3021', 'PUBLIC', 'STEM', 
     94.50, 2024, 'Zone 2 Mabini St.', 'Barangay Zone 1', 'Talisay City', 'Negros Occidental', '6115', 
     'Teodora Alonso', 'Mother', '+639189998877', 
     TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, 'POOR_BELOW_10K', TRUE, 
     'ENROLLED', TRUE, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 22 DAY), 92.50, 'Passed with Distinction in Quantitative Reasoning', 95.00, 'Highly motivated for Computer Science', 
     (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_cs' LIMIT 1)),

    (11, 'ADM-2026-0002', (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM entrance_exam_slots WHERE venue_room = 'University Amphitheater A' LIMIT 1), 
     'Angela', 'Bautista', 'Dalisay', NULL, '2006-03-25', 'Bacolod City', 
     'FEMALE', 'CISGENDER_FEMALE', 'SINGLE', 'FILIPINO', '+639174445566', 'angela.dalisay@gmail.com', 
     '987654321098', 'Bacolod City National High School', 'DEPED-3022', 'PUBLIC', 'TVL-ICT', 
     92.00, 2024, 'Purok Paghidaet', 'Barangay Taculing', 'Bacolod City', 'Negros Occidental', '6100', 
     'Bernardo Dalisay', 'Father', '+639187776655', 
     FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, 'LOW_INCOME_10K_TO_20K', TRUE, 
     'ENROLLED', TRUE, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 22 DAY), 88.00, 'Satisfactory technical problem-solving logic', 90.00, 'Clear aptitude for Information Technology', 
     (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), (SELECT id FROM users WHERE username = 'chair_it' LIMIT 1))
AS new_adm ON DUPLICATE KEY UPDATE 
    application_status = new_adm.application_status,
    is_enrolled = new_adm.is_enrolled,
    enrolled_at = new_adm.enrolled_at;

-- -----------------------------------------------------------------------------
-- 7. STUDENT PROFILES (15 DIVERSE REALISTIC PERSONAS)
-- Includes: INCOMING_FIRST_YEAR, TRANSFEREE, CONTINUING, and Admission Linkage
-- -----------------------------------------------------------------------------
INSERT INTO student_profiles (
    id, user_id, admission_application_id, student_number, 
    first_name, middle_name, last_name, suffix,
    program_id, curriculum_id, year_level, 
    enrollment_status, student_classification, is_graduating, 
    total_units_earned, cumulative_gpa, financial_clearance, departmental_clearance
) VALUES 
    -- Student A: Juan Dela Cruz (BSCS 3rd Year, Regular, 4Ps & TES Scholar, Cleared)
    (10, (SELECT id FROM users WHERE username = 'student_a' LIMIT 1), (SELECT id FROM admission_applications WHERE application_number = 'ADM-2026-0001' LIMIT 1), 
     '2026-0001', 'Juan', 'Protasio', 'Dela Cruz', NULL, 
     (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), 3, 
     'REGULAR', 'CONTINUING', FALSE, 72.00, 1.25, 'CLEARED', 'CLEARED'),
    
    -- Student B: Maria Santos (BSCS 3rd Year, Irregular, Solo Parent Household, Overdue Financial Balance)
    (11, (SELECT id FROM users WHERE username = 'student_b' LIMIT 1), NULL, 
     '2026-0002', 'Maria', 'Clara', 'Santos', NULL, 
     (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), 3, 
     'IRREGULAR', 'CONTINUING', FALSE, 54.00, 2.50, 'HOLD', 'CLEARED'),
    
    -- Student C: Paolo Reyes (BSCS 4th Year, Regular Graduating Senior, PWD Visual, GIDA, Magna Cum Laude)
    (12, (SELECT id FROM users WHERE username = 'student_c' LIMIT 1), NULL, 
     '2026-0003', 'Paolo', 'Inocencio', 'Reyes', NULL, 
     (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), 4, 
     'REGULAR', 'CONTINUING', TRUE, 126.00, 1.40, 'CLEARED', 'CLEARED'),
    
    -- Student D: Angela Dalisay (BSIT 2nd Year, Regular, Farmer/Fisherfolk RSBSA child, Cleared)
    (13, (SELECT id FROM users WHERE username = 'student_d' LIMIT 1), (SELECT id FROM admission_applications WHERE application_number = 'ADM-2026-0002' LIMIT 1), 
     '2026-0004', 'Angela', 'Bautista', 'Dalisay', NULL, 
     (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), 2, 
     'REGULAR', 'CONTINUING', FALSE, 36.00, 1.65, 'CLEARED', 'CLEARED'),
    
    -- Student E: Carlos Tan (BSCpE 3rd Year, Regular, First-Generation College, Cleared)
    (14, (SELECT id FROM users WHERE username = 'student_e' LIMIT 1), NULL, 
     '2026-0005', 'Carlos', 'Chua', 'Tan', 'Jr.', 
     (SELECT id FROM programs WHERE code = 'BSCpE' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCpE-2026' LIMIT 1), 3, 
     'REGULAR', 'CONTINUING', FALSE, 80.00, 1.75, 'CLEARED', 'CLEARED'),
    
    -- Student F: Fatima Hassan (BSBA-FM 4th Year, Regular Graduating, Indigenous People - Panay Bukidnon, Cum Laude)
    (15, (SELECT id FROM users WHERE username = 'student_f' LIMIT 1), NULL, 
     '2026-0006', 'Fatima', 'Alonto', 'Hassan', NULL, 
     (SELECT id FROM programs WHERE code = 'BSBA-FM' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSBA-FM-2026' LIMIT 1), 4, 
     'REGULAR', 'CONTINUING', TRUE, 120.00, 1.55, 'CLEARED', 'CLEARED'),
    
    -- Student G: Joshua Lim (BSED-MATH 2nd Year, Regular, Valedictorian Scholar, Cleared)
    (16, (SELECT id FROM users WHERE username = 'student_g' LIMIT 1), NULL, 
     '2026-0007', 'Joshua', 'Sy', 'Lim', NULL, 
     (SELECT id FROM programs WHERE code = 'BSED-MATH' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSED-MATH-2026' LIMIT 1), 2, 
     'REGULAR', 'CONTINUING', FALSE, 38.00, 1.15, 'CLEARED', 'CLEARED'),
    
    -- Student H: Gabriel Mendoza (BSCRIM 3rd Year, Irregular, Working Student, Non-FHE Repeat Course, Partial Settle)
    (17, (SELECT id FROM users WHERE username = 'student_h' LIMIT 1), NULL, 
     '2026-0008', 'Gabriel', 'Villanueva', 'Mendoza', NULL, 
     (SELECT id FROM programs WHERE code = 'BSCRIM' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCRIM-2026' LIMIT 1), 3, 
     'IRREGULAR', 'CONTINUING', FALSE, 68.00, 2.75, 'HOLD', 'CLEARED'),
    
    -- Student I: Bea Alonzo (BSCS 1st Year, Regular Freshman, 4Ps Beneficiary, Cleared)
    (18, (SELECT id FROM users WHERE username = 'student_i' LIMIT 1), NULL, 
     '2026-0009', 'Bea', 'Ramos', 'Alonzo', NULL, 
     (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), 1, 
     'REGULAR', 'INCOMING_FIRST_YEAR', FALSE, 0.00, 1.00, 'CLEARED', 'CLEARED'),
    
    -- Student J: Rafael Nadal (BSIT 4th Year, Regular Graduating, Transferee with Credited Subjects, Cleared)
    (19, (SELECT id FROM users WHERE username = 'student_j' LIMIT 1), NULL, 
     '2026-0010', 'Rafael', 'Parera', 'Nadal', NULL, 
     (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), 4, 
     'REGULAR', 'TRANSFEREE', TRUE, 115.00, 1.80, 'CLEARED', 'CLEARED'),
    
    -- Student K: Katrina Halili (BSA 2nd Year, Regular, Cleared)
    (20, (SELECT id FROM users WHERE username = 'student_k' LIMIT 1), NULL, 
     '2026-0011', 'Katrina', 'Gomez', 'Halili', NULL, 
     (SELECT id FROM programs WHERE code = 'BSA' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSA-2026' LIMIT 1), 2, 
     'REGULAR', 'CONTINUING', FALSE, 42.00, 1.50, 'CLEARED', 'CLEARED'),
    
    -- Student L: Marco Polo (BSHM 3rd Year, Regular, Cleared)
    (21, (SELECT id FROM users WHERE username = 'student_l' LIMIT 1), NULL, 
     '2026-0012', 'Marco', 'Sotto', 'Polo', NULL, 
     (SELECT id FROM programs WHERE code = 'BSHM' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSHM-2026' LIMIT 1), 3, 
     'REGULAR', 'CONTINUING', FALSE, 75.00, 2.00, 'CLEARED', 'CLEARED'),
    
    -- Student M: Dingdong Dantes (BSCE 4th Year, Regular Graduating, Cleared)
    (22, (SELECT id FROM users WHERE username = 'student_m' LIMIT 1), NULL, 
     '2026-0013', 'Dingdong', 'Gonzalez', 'Dantes', 'III', 
     (SELECT id FROM programs WHERE code = 'BSCE' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCE-2026' LIMIT 1), 4, 
     'REGULAR', 'CONTINUING', TRUE, 135.00, 1.70, 'CLEARED', 'CLEARED'),
    
    -- Student N: Marian Rivera (BEED 1st Year, Regular Freshman, Cleared)
    (23, (SELECT id FROM users WHERE username = 'student_n' LIMIT 1), NULL, 
     '2026-0014', 'Marian', 'Gracia', 'Rivera', NULL, 
     (SELECT id FROM programs WHERE code = 'BEED' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BEED-2026' LIMIT 1), 1, 
     'REGULAR', 'INCOMING_FIRST_YEAR', FALSE, 0.00, 1.00, 'CLEARED', 'CLEARED'),
    
    -- Student O: Alden Richards (BSECE 3rd Year, Irregular, Maximum Residency Rule Exceeded, Non-FHE Self-Paying)
    (24, (SELECT id FROM users WHERE username = 'student_o' LIMIT 1), NULL, 
     '2026-0015', 'Alden', 'Faulkerson', 'Richards', NULL, 
     (SELECT id FROM programs WHERE code = 'BSECE' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSECE-2026' LIMIT 1), 3, 
     'IRREGULAR', 'CONTINUING', FALSE, 90.00, 2.85, 'HOLD', 'CLEARED')
AS new_stud ON DUPLICATE KEY UPDATE 
    first_name = new_stud.first_name,
    last_name = new_stud.last_name,
    admission_application_id = new_stud.admission_application_id,
    student_classification = new_stud.student_classification,
    cumulative_gpa = new_stud.cumulative_gpa,
    financial_clearance = new_stud.financial_clearance;

-- Update Student Profile Foreign Key in Admission Applications
UPDATE admission_applications 
SET student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1)
WHERE application_number = 'ADM-2026-0001';

UPDATE admission_applications 
SET student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0004' LIMIT 1)
WHERE application_number = 'ADM-2026-0002';

-- -----------------------------------------------------------------------------
-- 8. CLASS SECTIONS, CONFLICT-FREE TIMETABLE SCHEDULES & ENROLLMENT
-- -----------------------------------------------------------------------------
INSERT INTO class_sections (id, term_id, curriculum_id, course_id, section_code, max_capacity, enrolled_count, status, grade_status, primary_instructor_id)
VALUES 
    -- CCS Sections
    (10, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS102' LIMIT 1),  'BSCS-1A', 40, 2, 'OPEN', 'DRAFT',  (SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1)),
    (11, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS301' LIMIT 1),  'BSCS-2A', 40, 2, 'OPEN', 'DRAFT',  (SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1)),
    (12, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CC105' LIMIT 1),  'BSCS-2B', 40, 2, 'OPEN', 'DRAFT',  (SELECT id FROM users WHERE username = 'faculty_tan' LIMIT 1)),
    (13, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS302' LIMIT 1),  'BSCS-3A', 40, 3, 'OPEN', 'DRAFT',  (SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1)),
    (14, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS401' LIMIT 1),  'BSCS-4A', 40, 1, 'OPEN', 'SEALED', (SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1)),
    (15, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT202' LIMIT 1),  'BSIT-2A', 40, 1, 'OPEN', 'DRAFT',  (SELECT id FROM users WHERE username = 'faculty_tan' LIMIT 1)),
    (16, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT401' LIMIT 1),  'BSIT-4A', 40, 1, 'OPEN', 'DRAFT',  (SELECT id FROM users WHERE username = 'faculty_tan' LIMIT 1)),

    -- COE Sections
    (17, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCpE-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CPE201' LIMIT 1), 'BSCpE-2A', 35, 1, 'OPEN', 'DRAFT', (SELECT id FROM users WHERE username = 'faculty_garcia' LIMIT 1)),
    (18, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCpE-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CPE301' LIMIT 1), 'BSCpE-3A', 35, 1, 'OPEN', 'DRAFT', (SELECT id FROM users WHERE username = 'faculty_garcia' LIMIT 1)),
    (19, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCE-2026' LIMIT 1),  (SELECT id FROM courses WHERE code = 'CE301' LIMIT 1),  'BSCE-3A',  30, 1, 'OPEN', 'DRAFT', (SELECT id FROM users WHERE username = 'faculty_garcia' LIMIT 1)),

    -- CBMA Sections
    (20, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSBA-FM-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'FM201' LIMIT 1), 'BSBA-2A', 50, 1, 'OPEN', 'DRAFT', (SELECT id FROM users WHERE username = 'faculty_reyes' LIMIT 1)),
    (21, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSA-2026' LIMIT 1),     (SELECT id FROM courses WHERE code = 'ACT201' LIMIT 1), 'BSA-2A',  40, 1, 'OPEN', 'DRAFT', (SELECT id FROM users WHERE username = 'faculty_reyes' LIMIT 1)),

    -- COED, CCJ & General Education
    (22, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSED-MATH-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'ED201' LIMIT 1),   'BSED-2A',     45, 1, 'OPEN', 'DRAFT', (SELECT id FROM users WHERE username = 'faculty_delacruz' LIMIT 1)),
    (23, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCRIM-2026' LIMIT 1),    (SELECT id FROM courses WHERE code = 'CRIM201' LIMIT 1), 'BCRIM-2A',    45, 1, 'OPEN', 'DRAFT', (SELECT id FROM users WHERE username = 'faculty_villanueva' LIMIT 1)),
    (24, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1),      (SELECT id FROM courses WHERE code = 'GEC101' LIMIT 1),  'GEC-1A',      50, 4, 'OPEN', 'DRAFT', (SELECT id FROM users WHERE username = 'faculty_aquino' LIMIT 1)),
    (25, (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1),      (SELECT id FROM courses WHERE code = 'GEC-RIZAL' LIMIT 1), 'GEC-RIZAL-A', 50, 3, 'OPEN', 'DRAFT', (SELECT id FROM users WHERE username = 'faculty_aquino' LIMIT 1))
AS new_sec ON DUPLICATE KEY UPDATE section_code = new_sec.section_code, grade_status = new_sec.grade_status;

-- Conflict-Free Master Schedules (Distinct times, rooms, and faculty)
INSERT INTO class_schedules (id, section_id, room_id, instructor_user_id, day_of_week, start_time, end_time, schedule_type)
VALUES 
    (10, (SELECT id FROM class_sections WHERE section_code = 'BSCS-1A' LIMIT 1),     (SELECT id FROM rooms WHERE code = 'CLAB-1' LIMIT 1),   (SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1),      'MONDAY',    '08:00:00', '11:00:00', 'LABORATORY'),
    (11, (SELECT id FROM class_sections WHERE section_code = 'BSCS-2A' LIMIT 1),     (SELECT id FROM rooms WHERE code = 'CLAB-1' LIMIT 1),   (SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1),      'TUESDAY',   '08:00:00', '11:00:00', 'LABORATORY'),
    (12, (SELECT id FROM class_sections WHERE section_code = 'BSCS-2B' LIMIT 1),     (SELECT id FROM rooms WHERE code = 'CLAB-2' LIMIT 1),   (SELECT id FROM users WHERE username = 'faculty_tan' LIMIT 1),        'WEDNESDAY', '08:00:00', '11:00:00', 'LABORATORY'),
    (13, (SELECT id FROM class_sections WHERE section_code = 'BSCS-3A' LIMIT 1),     (SELECT id FROM rooms WHERE code = 'CLAB-1' LIMIT 1),   (SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1),      'THURSDAY',  '08:00:00', '11:00:00', 'LABORATORY'),
    (14, (SELECT id FROM class_sections WHERE section_code = 'BSCS-4A' LIMIT 1),     (SELECT id FROM rooms WHERE code = 'LEC-201' LIMIT 1),  (SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1),      'FRIDAY',    '08:00:00', '11:00:00', 'LECTURE'),
    (15, (SELECT id FROM class_sections WHERE section_code = 'BSIT-2A' LIMIT 1),     (SELECT id FROM rooms WHERE code = 'CLAB-2' LIMIT 1),   (SELECT id FROM users WHERE username = 'faculty_tan' LIMIT 1),        'MONDAY',    '13:00:00', '16:00:00', 'LABORATORY'),
    (16, (SELECT id FROM class_sections WHERE section_code = 'BSIT-4A' LIMIT 1),     (SELECT id FROM rooms WHERE code = 'LEC-201' LIMIT 1),  (SELECT id FROM users WHERE username = 'faculty_tan' LIMIT 1),        'TUESDAY',   '13:00:00', '16:00:00', 'LECTURE'),
    (17, (SELECT id FROM class_sections WHERE section_code = 'BSCpE-2A' LIMIT 1),    (SELECT id FROM rooms WHERE code = 'ENG-LAB1' LIMIT 1), (SELECT id FROM users WHERE username = 'faculty_garcia' LIMIT 1),     'WEDNESDAY', '13:00:00', '16:00:00', 'LABORATORY'),
    (18, (SELECT id FROM class_sections WHERE section_code = 'BSCpE-3A' LIMIT 1),    (SELECT id FROM rooms WHERE code = 'ENG-LAB1' LIMIT 1), (SELECT id FROM users WHERE username = 'faculty_garcia' LIMIT 1),     'THURSDAY',  '13:00:00', '16:00:00', 'LABORATORY'),
    (19, (SELECT id FROM class_sections WHERE section_code = 'BSCE-3A' LIMIT 1),     (SELECT id FROM rooms WHERE code = 'LEC-202' LIMIT 1),  (SELECT id FROM users WHERE username = 'faculty_garcia' LIMIT 1),     'FRIDAY',    '13:00:00', '16:00:00', 'LECTURE'),
    (20, (SELECT id FROM class_sections WHERE section_code = 'BSBA-2A' LIMIT 1),     (SELECT id FROM rooms WHERE code = 'CBMA-AUD' LIMIT 1), (SELECT id FROM users WHERE username = 'faculty_reyes' LIMIT 1),      'MONDAY',    '09:00:00', '12:00:00', 'LECTURE'),
    (21, (SELECT id FROM class_sections WHERE section_code = 'BSA-2A' LIMIT 1),      (SELECT id FROM rooms WHERE code = 'CBMA-AUD' LIMIT 1), (SELECT id FROM users WHERE username = 'faculty_reyes' LIMIT 1),      'TUESDAY',   '09:00:00', '12:00:00', 'LECTURE'),
    (22, (SELECT id FROM class_sections WHERE section_code = 'BSED-2A' LIMIT 1),     (SELECT id FROM rooms WHERE code = 'LEC-202' LIMIT 1),  (SELECT id FROM users WHERE username = 'faculty_delacruz' LIMIT 1),   'WEDNESDAY', '09:00:00', '12:00:00', 'LECTURE'),
    (23, (SELECT id FROM class_sections WHERE section_code = 'BCRIM-2A' LIMIT 1),    (SELECT id FROM rooms WHERE code = 'CRIM-MOOT' LIMIT 1),(SELECT id FROM users WHERE username = 'faculty_villanueva' LIMIT 1), 'THURSDAY',  '09:00:00', '12:00:00', 'LECTURE'),
    (24, (SELECT id FROM class_sections WHERE section_code = 'GEC-1A' LIMIT 1),      (SELECT id FROM rooms WHERE code = 'LEC-201' LIMIT 1),  (SELECT id FROM users WHERE username = 'faculty_aquino' LIMIT 1),     'SATURDAY',  '08:00:00', '11:00:00', 'LECTURE'),
    (25, (SELECT id FROM class_sections WHERE section_code = 'GEC-RIZAL-A' LIMIT 1),(SELECT id FROM rooms WHERE code = 'LEC-201' LIMIT 1),  (SELECT id FROM users WHERE username = 'faculty_aquino' LIMIT 1),     'SATURDAY',  '13:00:00', '16:00:00', 'LECTURE')
AS new_sched ON DUPLICATE KEY UPDATE start_time = new_sched.start_time;

-- Student Term Enrollments (Dynamic Timestamps)
INSERT INTO student_enrollments (id, student_id, term_id, enrollment_date, status, total_credit_units, is_overload_approved)
VALUES 
    (10, (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 1 HOUR, 'ENROLLED', 6.00, FALSE),
    (11, (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 2 HOUR, 'ENROLLED', 6.00, FALSE),
    (12, (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 3 HOUR, 'ENROLLED', 6.00, FALSE),
    (13, (SELECT id FROM student_profiles WHERE student_number = '2026-0004' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 4 HOUR, 'ENROLLED', 6.00, FALSE),
    (14, (SELECT id FROM student_profiles WHERE student_number = '2026-0005' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 5 HOUR, 'ENROLLED', 6.00, FALSE),
    (15, (SELECT id FROM student_profiles WHERE student_number = '2026-0006' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 6 HOUR, 'ENROLLED', 6.00, FALSE),
    (16, (SELECT id FROM student_profiles WHERE student_number = '2026-0007' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 7 HOUR, 'ENROLLED', 6.00, FALSE),
    (17, (SELECT id FROM student_profiles WHERE student_number = '2026-0008' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 8 HOUR, 'ENROLLED', 6.00, FALSE),
    (18, (SELECT id FROM student_profiles WHERE student_number = '2026-0009' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 9 HOUR, 'ENROLLED', 6.00, FALSE),
    (19, (SELECT id FROM student_profiles WHERE student_number = '2026-0010' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 10 HOUR, 'ENROLLED', 6.00, FALSE),
    (20, (SELECT id FROM student_profiles WHERE student_number = '2026-0011' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 11 HOUR, 'ENROLLED', 6.00, FALSE),
    (21, (SELECT id FROM student_profiles WHERE student_number = '2026-0012' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 12 HOUR, 'ENROLLED', 6.00, FALSE),
    (22, (SELECT id FROM student_profiles WHERE student_number = '2026-0013' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 13 HOUR, 'ENROLLED', 6.00, FALSE),
    (23, (SELECT id FROM student_profiles WHERE student_number = '2026-0014' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 14 HOUR, 'ENROLLED', 6.00, FALSE),
    (24, (SELECT id FROM student_profiles WHERE student_number = '2026-0015' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 25 DAY) + INTERVAL 15 HOUR, 'ENROLLED', 6.00, FALSE)
AS new_enr ON DUPLICATE KEY UPDATE status = new_enr.status, total_credit_units = new_enr.total_credit_units;

-- Enrolled Line Items
INSERT INTO enrollment_course_items (id, enrollment_id, section_id, final_numerical_grade, completion_status)
VALUES 
    -- Juan Dela Cruz enrolled in BSCS-3A & GEC-RIZAL-A
    (10, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'BSCS-3A' LIMIT 1), 1.25, 'PASSED'),
    (11, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'GEC-RIZAL-A' LIMIT 1), 1.25, 'PASSED'),
    -- Maria Santos enrolled in BSCS-3A & GEC-RIZAL-A
    (12, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'BSCS-3A' LIMIT 1), 2.50, 'PASSED'),
    (13, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'GEC-RIZAL-A' LIMIT 1), 2.25, 'PASSED'),
    -- Paolo Reyes enrolled in BSCS-4A & GEC-RIZAL-A (Sealed Section)
    (14, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'BSCS-4A' LIMIT 1), 1.25, 'PASSED'),
    (15, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'GEC-RIZAL-A' LIMIT 1), 1.50, 'PASSED'),
    -- Angela Dalisay enrolled in BSIT-2A & GEC-1A
    (16, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0004' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'BSIT-2A' LIMIT 1), 1.50, 'PASSED'),
    (17, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0004' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'GEC-1A' LIMIT 1), 1.75, 'PASSED'),
    -- Carlos Tan enrolled in BSCpE-3A & GEC-1A
    (18, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0005' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'BSCpE-3A' LIMIT 1), 1.75, 'PASSED'),
    (19, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0005' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'GEC-1A' LIMIT 1), 1.75, 'PASSED'),
    -- Fatima Hassan enrolled in BSBA-2A & GEC-1A
    (20, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0006' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'BSBA-2A' LIMIT 1), 1.50, 'PASSED'),
    (21, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0006' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'GEC-1A' LIMIT 1), 1.50, 'PASSED'),
    -- Joshua Lim enrolled in BSED-2A
    (22, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0007' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'BSED-2A' LIMIT 1), 1.25, 'PASSED'),
    -- Gabriel Mendoza enrolled in BCRIM-2A (Repeat Course)
    (23, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0008' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'BCRIM-2A' LIMIT 1), 2.75, 'PASSED'),
    -- Bea Alonzo enrolled in BSCS-1A
    (24, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0009' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'BSCS-1A' LIMIT 1), 1.50, 'PASSED'),
    -- Rafael Nadal enrolled in BSIT-4A
    (25, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0010' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'BSIT-4A' LIMIT 1), 1.75, 'PASSED'),
    -- Alden Richards enrolled in BSCpE-2A (MRR Exceeded)
    (26, (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0015' LIMIT 1) LIMIT 1), (SELECT id FROM class_sections WHERE section_code = 'BSCpE-2A' LIMIT 1), 3.00, 'PASSED')
AS new_item ON DUPLICATE KEY UPDATE final_numerical_grade = new_item.final_numerical_grade, completion_status = new_item.completion_status;

-- Transferee Crediting (CMO 25 Validation for Rafael Nadal)
INSERT INTO course_equivalencies (id, student_id, external_institution, external_course_code, external_course_title, internal_course_id, external_numerical_grade, credits_granted, status, approved_by_user_id, remarks)
VALUES 
    (10, (SELECT id FROM student_profiles WHERE student_number = '2026-0010' LIMIT 1), 'University of St. La Salle - Bacolod', 'CS101-USLS', 'Introduction to Computer Concepts', (SELECT id FROM courses WHERE code = 'CS101' LIMIT 1), 1.25, 3.00, 'APPROVED', (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), 'Full credit mapping granted per CMO 25 validation'),
    (11, (SELECT id FROM student_profiles WHERE student_number = '2026-0010' LIMIT 1), 'University of St. La Salle - Bacolod', 'ENG101-USLS', 'College English Communication',     (SELECT id FROM courses WHERE code = 'GEC101' LIMIT 1), 1.50, 3.00, 'APPROVED', (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), 'General Education credited per CHED CMO 20 s. 2013')
AS new_equiv ON DUPLICATE KEY UPDATE status = new_equiv.status;

-- -----------------------------------------------------------------------------
-- 9. DYNAMIC CLASS RECORDS, ASSESSMENT SCORES & GRADE SEALING
-- -----------------------------------------------------------------------------
INSERT INTO section_grading_configs (id, section_id, midterm_weight, final_weight, is_locked)
VALUES 
    (10, (SELECT id FROM class_sections WHERE section_code = 'BSCS-3A' LIMIT 1), 50.00, 50.00, FALSE),
    (11, (SELECT id FROM class_sections WHERE section_code = 'BSCS-4A' LIMIT 1), 50.00, 50.00, TRUE)
AS new_cfg ON DUPLICATE KEY UPDATE is_locked = new_cfg.is_locked;

INSERT INTO section_grading_categories (id, config_id, category_name, weight_percentage, term_period, display_order)
VALUES 
    (10, (SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-3A' LIMIT 1) LIMIT 1), 'Quizzes & Exercises', 20.00, 'MIDTERM', 1),
    (11, (SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-3A' LIMIT 1) LIMIT 1), 'Machine Problems',     30.00, 'MIDTERM', 2),
    (12, (SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-3A' LIMIT 1) LIMIT 1), 'Periodic Examination', 50.00, 'MIDTERM', 3),
    (13, (SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-4A' LIMIT 1) LIMIT 1), 'Quizzes & Exercises', 20.00, 'MIDTERM', 1),
    (14, (SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-4A' LIMIT 1) LIMIT 1), 'Capstone Artifacts',   30.00, 'MIDTERM', 2),
    (15, (SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-4A' LIMIT 1) LIMIT 1), 'Midterm Defense',     50.00, 'MIDTERM', 3)
AS new_cat ON DUPLICATE KEY UPDATE weight_percentage = new_cat.weight_percentage;

INSERT INTO class_record_items (id, category_id, item_title, max_points, sequence_order)
VALUES 
    (10, (SELECT id FROM section_grading_categories WHERE config_id = (SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-3A' LIMIT 1) LIMIT 1) AND category_name = 'Quizzes & Exercises' LIMIT 1), 'Quiz 1 - Agile & Scrum Ceremonies', 20.00, 1),
    (11, (SELECT id FROM section_grading_categories WHERE config_id = (SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-3A' LIMIT 1) LIMIT 1) AND category_name = 'Machine Problems' LIMIT 1),     'MP 1 - UML Domain Architecture & ERD', 50.00, 1),
    (12, (SELECT id FROM section_grading_categories WHERE config_id = (SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-3A' LIMIT 1) LIMIT 1) AND category_name = 'Periodic Examination' LIMIT 1), 'Midterm Departmental Examination', 100.00, 1),
    (13, (SELECT id FROM section_grading_categories WHERE config_id = (SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-4A' LIMIT 1) LIMIT 1) AND category_name = 'Quizzes & Exercises' LIMIT 1), 'Proposal Assessment', 20.00, 1),
    (14, (SELECT id FROM section_grading_categories WHERE config_id = (SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-4A' LIMIT 1) LIMIT 1) AND category_name = 'Capstone Artifacts' LIMIT 1), 'Software Architecture Document', 50.00, 1),
    (15, (SELECT id FROM section_grading_categories WHERE config_id = (SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-4A' LIMIT 1) LIMIT 1) AND category_name = 'Midterm Defense' LIMIT 1), 'Oral Defense & Verification', 100.00, 1)
AS new_cri ON DUPLICATE KEY UPDATE max_points = new_cri.max_points;

INSERT INTO student_assessment_scores (id, item_id, student_id, score_earned, is_excused)
VALUES 
    (10, 10, (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1), 19.50, FALSE),
    (11, 11, (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1), 48.00, FALSE),
    (12, 12, (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1), 95.00, FALSE),
    (13, 10, (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1), 12.00, FALSE),
    (14, 11, (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1), 32.00, FALSE),
    (15, 12, (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1), 68.00, FALSE),
    (16, 13, (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1), 19.00, FALSE),
    (17, 14, (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1), 47.00, FALSE),
    (18, 15, (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1), 94.00, FALSE)
AS new_score ON DUPLICATE KEY UPDATE score_earned = new_score.score_earned;

-- Cryptographic Registrar Grade Sealing Audit Ledger (Dynamic Timestamp)
INSERT INTO grade_sealing_audits (id, section_id, registrar_user_id, student_records_sealed, section_code, course_code, sealed_at, checksum_hash)
VALUES (
    10, 
    (SELECT id FROM class_sections WHERE section_code = 'BSCS-4A' LIMIT 1), 
    (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), 
    1, 
    'BSCS-4A', 
    'CS401', 
    DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 DAY), 
    'a4f91b72e539d09c3132cf0b44558e1c6b889d1469e88ef211029c9a00a12b55'
)
AS new_gsa ON DUPLICATE KEY UPDATE checksum_hash = new_gsa.checksum_hash;

-- -----------------------------------------------------------------------------
-- 10. PHILIPPINE SUC FINANCIAL SUBSYSTEM (RA 10931, COA FORM 51, FUND CLUSTERS)
-- -----------------------------------------------------------------------------
INSERT INTO fund_clusters (id, code, name, description, bank_account_number, depository_bank_name, active)
VALUES 
    (1, 'FUND_101', 'General Fund (Regular Agency Fund - MDS)', 'National Government Subsidy through DBM Modified Disbursement System', 'MDS-101-0001', 'Bureau of the Treasury / Land Bank of the Philippines', TRUE),
    (2, 'FUND_164', 'Special Trust Fund (Internally Generated Income)', 'SUC Revolving Fund (RA 8292) for Tuition, TOSF, and Internally Generated Income', '1422-00164-88', 'Land Bank of the Philippines', TRUE),
    (3, 'FUND_184', 'Trust Receipts (Fiduciary & Student Funds)', 'Student government fees, alumni trust deposits, laboratory breakage deposits', '1422-00184-99', 'Land Bank of the Philippines', TRUE)
AS new_fc ON DUPLICATE KEY UPDATE name = new_fc.name;

INSERT INTO fee_templates (id, name, academic_year_id, campus_id, tuition_per_unit, lab_fee_per_unit, miscellaneous_flat_fee, athletic_flat_fee, is_active)
VALUES 
    (10, 'AY 2026-2027 Standard SUC Undergraduate Fee Template', (SELECT id FROM academic_years WHERE code = 'AY-2026-2027' LIMIT 1), (SELECT id FROM campuses WHERE code = 'TALISAY_MAIN' LIMIT 1), 300.00, 500.00, 1500.00, 500.00, TRUE)
AS new_ft ON DUPLICATE KEY UPDATE tuition_per_unit = new_ft.tuition_per_unit;

-- Physical COA Form 51 Official Receipt Booklet Inventory
INSERT INTO or_booklets (id, booklet_code, start_or_number, end_or_number, current_or_number, assigned_cashier_id, status)
VALUES 
    (10, 'BKL-2026-001', 'OR-2026-0001', 'OR-2026-0050', 'OR-2026-0005', (SELECT id FROM users WHERE username = 'cashier_main' LIMIT 1), 'ACTIVE'),
    (11, 'BKL-2026-002', 'OR-2026-0051', 'OR-2026-0100', 'OR-2026-0051', (SELECT id FROM users WHERE username = 'cashier_ft' LIMIT 1),   'ACTIVE'),
    (12, 'BKL-2026-003', 'OR-2026-0101', 'OR-2026-0150', 'OR-2026-0101', (SELECT id FROM users WHERE username = 'cashier_main' LIMIT 1), 'ACTIVE')
AS new_bk ON DUPLICATE KEY UPDATE current_or_number = new_bk.current_or_number;

-- Voided / Spoiled Official Receipt Entry (Dynamic Timestamp)
INSERT INTO voided_official_receipts (id, or_number, booklet_id, voided_by_cashier_id, void_reason, voided_at)
VALUES (
    10, 
    'OR-2026-0004', 
    (SELECT id FROM or_booklets WHERE booklet_code = 'BKL-2026-001' LIMIT 1), 
    (SELECT id FROM users WHERE username = 'cashier_main' LIMIT 1), 
    'Spoiled Form 51 - printer jam error on serial alignment', 
    DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 15 DAY)
)
AS new_vor ON DUPLICATE KEY UPDATE void_reason = new_vor.void_reason;

-- Student Assessment Invoices
INSERT INTO student_assessment_invoices (
    id, invoice_number, student_enrollment_id, student_profile_id, term_id, 
    total_tuition_fee, total_lab_fee, total_misc_fee, total_gross_assessment, 
    fhe_subsidy_amount, scholarship_discount_amount, net_assessed_amount, 
    total_paid_amount, outstanding_balance, status, is_fhe_eligible
) VALUES 
    -- Juan Dela Cruz (FHE Eligible: 100% Free Higher Education Subsidy)
    (10, 'INV-2026-0001', 
     (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1) LIMIT 1), 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1), 
     (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     1800.00, 1000.00, 2000.00, 4800.00, 4800.00, 0.00, 0.00, 0.00, 0.00, 'FHE_COVERED', TRUE),
    
    -- Maria Santos (FHE Disqualified - Repeat Subject personal assessment, Partial Settlement)
    (11, 'INV-2026-0002', 
     (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1) LIMIT 1), 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1), 
     (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     1800.00, 1000.00, 2000.00, 4800.00, 0.00, 0.00, 4800.00, 1000.00, 3800.00, 'PARTIAL', FALSE),
    
    -- Paolo Reyes (FHE Eligible: 100% Free Higher Education Subsidy)
    (12, 'INV-2026-0003', 
     (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1) LIMIT 1), 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1), 
     (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     1800.00, 1000.00, 2000.00, 4800.00, 4800.00, 0.00, 0.00, 0.00, 0.00, 'FHE_COVERED', TRUE),
    
    -- Gabriel Mendoza (Repeat subject: Partial settlement with check)
    (13, 'INV-2026-0004', 
     (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0008' LIMIT 1) LIMIT 1), 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0008' LIMIT 1), 
     (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     1800.00, 1000.00, 2000.00, 4800.00, 0.00, 0.00, 4800.00, 2000.00, 2800.00, 'PARTIAL', FALSE),
    
    -- Alden Richards (MRR Exceeded: 100% Personal Liability, Unpaid)
    (14, 'INV-2026-0005', 
     (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0015' LIMIT 1) LIMIT 1), 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0015' LIMIT 1), 
     (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     1800.00, 1000.00, 2000.00, 4800.00, 0.00, 0.00, 4800.00, 0.00, 4800.00, 'UNPAID', FALSE)
AS new_inv ON DUPLICATE KEY UPDATE outstanding_balance = new_inv.outstanding_balance;

-- Double-Entry Student Account Ledgers (Dynamic Dates & Dynamic Lookups)
INSERT INTO student_account_ledgers (
    id, transaction_number, student_profile_id, term_id, assessment_invoice_id, 
    transaction_type, transaction_date, description, debit_amount, credit_amount, running_balance, 
    reference_number, fund_cluster_code, created_by_user_id
) VALUES 
    -- Juan Dela Cruz
    (10, 'TXN-2026-0001', 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     (SELECT id FROM student_assessment_invoices WHERE invoice_number = 'INV-2026-0001' LIMIT 1), 
     'CHARGE', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 24 DAY), 'Gross Assessment per Form 1', 4800.00, 0.00, 4800.00, 'INV-2026-0001', 'FUND_101', 
     (SELECT id FROM users WHERE username = 'accountant_head' LIMIT 1)),
    
    (11, 'TXN-2026-0002', 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     (SELECT id FROM student_assessment_invoices WHERE invoice_number = 'INV-2026-0001' LIMIT 1), 
     'FHE_SUBSIDY', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 24 DAY) + INTERVAL 1 HOUR, 'RA 10931 Free Higher Education Subsidy', 0.00, 4800.00, 0.00, 'FHE-2026-001', 'FUND_101', 
     (SELECT id FROM users WHERE username = 'accountant_head' LIMIT 1)),
    
    -- Maria Santos
    (12, 'TXN-2026-0003', 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     (SELECT id FROM student_assessment_invoices WHERE invoice_number = 'INV-2026-0002' LIMIT 1), 
     'CHARGE', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 24 DAY), 'Repeat Subject Assessment', 4800.00, 0.00, 4800.00, 'INV-2026-0002', 'FUND_164', 
     (SELECT id FROM users WHERE username = 'accountant_head' LIMIT 1)),
    
    (13, 'TXN-2026-0004', 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     (SELECT id FROM student_assessment_invoices WHERE invoice_number = 'INV-2026-0002' LIMIT 1), 
     'PAYMENT', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 18 DAY), 'Cash Partial Payment Downpayment', 0.00, 1000.00, 3800.00, 'OR-2026-0001', 'FUND_164', 
     (SELECT id FROM users WHERE username = 'cashier_main' LIMIT 1)),
    
    -- Paolo Reyes
    (14, 'TXN-2026-0005', 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     (SELECT id FROM student_assessment_invoices WHERE invoice_number = 'INV-2026-0003' LIMIT 1), 
     'CHARGE', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 24 DAY), 'Gross Assessment per Form 1', 4800.00, 0.00, 4800.00, 'INV-2026-0003', 'FUND_101', 
     (SELECT id FROM users WHERE username = 'accountant_head' LIMIT 1)),
    
    (15, 'TXN-2026-0006', 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     (SELECT id FROM student_assessment_invoices WHERE invoice_number = 'INV-2026-0003' LIMIT 1), 
     'FHE_SUBSIDY', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 24 DAY) + INTERVAL 1 HOUR, 'RA 10931 Free Higher Education Subsidy', 0.00, 4800.00, 0.00, 'FHE-2026-002', 'FUND_101', 
     (SELECT id FROM users WHERE username = 'accountant_head' LIMIT 1)),
    
    -- Gabriel Mendoza
    (16, 'TXN-2026-0007', 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0008' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     (SELECT id FROM student_assessment_invoices WHERE invoice_number = 'INV-2026-0004' LIMIT 1), 
     'CHARGE', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 24 DAY), 'Repeat Course Assessment', 4800.00, 0.00, 4800.00, 'INV-2026-0004', 'FUND_164', 
     (SELECT id FROM users WHERE username = 'accountant_head' LIMIT 1)),
    
    (17, 'TXN-2026-0008', 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0008' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     (SELECT id FROM student_assessment_invoices WHERE invoice_number = 'INV-2026-0004' LIMIT 1), 
     'PAYMENT', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 16 DAY), 'LandBank Manager Check Payment', 0.00, 2000.00, 2800.00, 'OR-2026-0002', 'FUND_164', 
     (SELECT id FROM users WHERE username = 'cashier_main' LIMIT 1)),
    
    -- Alden Richards
    (18, 'TXN-2026-0009', 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0015' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     (SELECT id FROM student_assessment_invoices WHERE invoice_number = 'INV-2026-0005' LIMIT 1), 
     'CHARGE', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 24 DAY), 'Overstay MRR Full Assessment', 4800.00, 0.00, 4800.00, 'INV-2026-0005', 'FUND_164', 
     (SELECT id FROM users WHERE username = 'accountant_head' LIMIT 1))
AS new_ledg ON DUPLICATE KEY UPDATE running_balance = new_ledg.running_balance;

-- Cashier Receipts (Cash and Check with COA and Fund Cluster metadata)
INSERT INTO cashier_receipts (
    id, or_number, student_profile_id, assessment_invoice_id, 
    amount_tendered, amount_paid, change_amount, payment_method, 
    reference_number, check_number, drawee_bank, remarks, status, 
    cashier_user_id, fund_cluster_code, issued_at
) VALUES 
    (10, 'OR-2026-0001', 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1), 
     (SELECT id FROM student_assessment_invoices WHERE invoice_number = 'INV-2026-0002' LIMIT 1), 
     1000.00, 1000.00, 0.00, 'CASH', 'REF-PAY-001', NULL, NULL, 'Partial downpayment for enrollment', 'VALID', 
     (SELECT id FROM users WHERE username = 'cashier_main' LIMIT 1), 'FUND_164', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 18 DAY)),
    
    (11, 'OR-2026-0002', 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0008' LIMIT 1), 
     (SELECT id FROM student_assessment_invoices WHERE invoice_number = 'INV-2026-0004' LIMIT 1), 
     2000.00, 2000.00, 0.00, 'CHECK', 'REF-PAY-002', 'CHK-LBP-99120', 'Land Bank of the Philippines - Bacolod Branch', 'Manager Check payment for repeat course', 'VALID', 
     (SELECT id FROM users WHERE username = 'cashier_main' LIMIT 1), 'FUND_164', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 16 DAY))
AS new_rcpt ON DUPLICATE KEY UPDATE amount_paid = new_rcpt.amount_paid;

-- UniFAST Free Higher Education Billing Claims (RA 10931 Form 1 & Form 2)
INSERT INTO unifast_fhe_claims (
    id, claim_batch_number, term_id, campus_id, 
    total_beneficiaries, total_tuition_claimed, total_tosf_claimed, 
    total_claim_amount, status, submitted_at, created_by_user_id
) VALUES 
    (10, 'UNIFAST-FHE-AY2026-2027-001', 
     (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     (SELECT id FROM campuses WHERE code = 'TALISAY_MAIN' LIMIT 1), 
     2, 3600.00, 6000.00, 9600.00, 'SUBMITTED', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 10 DAY), 
     (SELECT id FROM users WHERE username = 'accountant_head' LIMIT 1))
AS new_claim ON DUPLICATE KEY UPDATE total_claim_amount = new_claim.total_claim_amount;

INSERT INTO unifast_fhe_claim_items (
    id, claim_batch_id, student_profile_id, assessment_invoice_id, 
    enrolled_units, tuition_amount, misc_amount, lab_amount, 
    total_claimed_amount, verification_status
) VALUES 
    (10, (SELECT id FROM unifast_fhe_claims WHERE claim_batch_number = 'UNIFAST-FHE-AY2026-2027-001' LIMIT 1), 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1), 
     (SELECT id FROM student_assessment_invoices WHERE invoice_number = 'INV-2026-0001' LIMIT 1), 
     6.00, 1800.00, 2000.00, 1000.00, 4800.00, 'VERIFIED'),
    
    (11, (SELECT id FROM unifast_fhe_claims WHERE claim_batch_number = 'UNIFAST-FHE-AY2026-2027-001' LIMIT 1), 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1), 
     (SELECT id FROM student_assessment_invoices WHERE invoice_number = 'INV-2026-0003' LIMIT 1), 
     6.00, 1800.00, 2000.00, 1000.00, 4800.00, 'VERIFIED')
AS new_citem ON DUPLICATE KEY UPDATE total_claimed_amount = new_citem.total_claimed_amount;

-- -----------------------------------------------------------------------------
-- 11. MULTI-DEPARTMENT CLEARANCE REQUESTS & GRADUATION AUDIT
-- Strictly enforces ALL 5 required departments: LIBRARY, ACCOUNTING, LABORATORY, STUDENT_AFFAIRS, DEAN
-- -----------------------------------------------------------------------------
INSERT INTO clearance_requests (id, student_profile_id, term_id, purpose, overall_status)
VALUES 
    (10, (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 'ENROLLMENT', 'CLEARED'),
    (11, (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 'ENROLLMENT', 'PENDING'),
    (12, (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 'GRADUATION', 'CLEARED'),
    (13, (SELECT id FROM student_profiles WHERE student_number = '2026-0006' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 'GRADUATION', 'CLEARED'),
    (14, (SELECT id FROM student_profiles WHERE student_number = '2026-0010' LIMIT 1), (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 'GRADUATION', 'PENDING')
AS new_clr ON DUPLICATE KEY UPDATE overall_status = new_clr.overall_status;

INSERT INTO clearance_signoffs (id, clearance_request_id, department_type, signoff_status, remarks, signed_by_user_id, signed_at)
VALUES 
    -- Juan Dela Cruz (Enrollment Clearance - Cleared)
    (101, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1) LIMIT 1), 'LIBRARY',         'APPROVED', 'No outstanding book borrowings',            (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (102, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1) LIMIT 1), 'ACCOUNTING',      'APPROVED', 'Fully covered under RA 10931 FHE',          (SELECT id FROM users WHERE username = 'accountant_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (103, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1) LIMIT 1), 'LABORATORY',      'APPROVED', 'No laboratory apparatus breakages',         (SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1),  DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (104, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1) LIMIT 1), 'STUDENT_AFFAIRS', 'APPROVED', 'Good moral character standing',              (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (105, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1) LIMIT 1), 'DEAN',            'APPROVED', 'Academic clearance validated',              (SELECT id FROM users WHERE username = 'dean_ccs' LIMIT 1),        DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),

    -- Maria Santos (Enrollment Clearance - Held by Accounting)
    (111, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1) LIMIT 1), 'LIBRARY',         'APPROVED', 'Cleared',                                   (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (112, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1) LIMIT 1), 'ACCOUNTING',      'REJECTED', 'Unpaid repeat course balance of P3,800.00', (SELECT id FROM users WHERE username = 'accountant_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (113, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1) LIMIT 1), 'LABORATORY',      'APPROVED', 'Cleared',                                   (SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1),  DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (114, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1) LIMIT 1), 'STUDENT_AFFAIRS', 'APPROVED', 'Cleared',                                   (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (115, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1) LIMIT 1), 'DEAN',            'PENDING',  NULL,                                        NULL, NULL),

    -- Paolo Reyes (Graduation Clearance - Cleared)
    (121, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1) LIMIT 1), 'LIBRARY',         'APPROVED', 'Cleared for graduation',                    (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (122, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1) LIMIT 1), 'ACCOUNTING',      'APPROVED', 'Zero balance verified',                     (SELECT id FROM users WHERE username = 'accountant_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (123, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1) LIMIT 1), 'LABORATORY',      'APPROVED', 'Capstone equipment surrendered',             (SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1),  DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (124, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1) LIMIT 1), 'STUDENT_AFFAIRS', 'APPROVED', 'Cleared',                                   (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (125, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1) LIMIT 1), 'DEAN',            'APPROVED', 'Degree audit 100% complete',                (SELECT id FROM users WHERE username = 'dean_ccs' LIMIT 1),        DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),

    -- Fatima Hassan (Graduation Clearance - Cleared)
    (131, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0006' LIMIT 1) LIMIT 1), 'LIBRARY',         'APPROVED', 'Cleared',                                   (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (132, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0006' LIMIT 1) LIMIT 1), 'ACCOUNTING',      'APPROVED', 'Cleared',                                   (SELECT id FROM users WHERE username = 'accountant_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (133, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0006' LIMIT 1) LIMIT 1), 'LABORATORY',      'APPROVED', 'Cleared',                                   (SELECT id FROM users WHERE username = 'faculty_reyes' LIMIT 1),  DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (134, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0006' LIMIT 1) LIMIT 1), 'STUDENT_AFFAIRS', 'APPROVED', 'Cleared',                                   (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (135, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0006' LIMIT 1) LIMIT 1), 'DEAN',            'APPROVED', 'Cleared for Commencement Exercises',         (SELECT id FROM users WHERE username = 'dean_cbma' LIMIT 1),       DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),

    -- Rafael Nadal (Graduation Clearance - Pending)
    (141, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0010' LIMIT 1) LIMIT 1), 'LIBRARY',         'APPROVED', 'Cleared',                                   (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (142, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0010' LIMIT 1) LIMIT 1), 'ACCOUNTING',      'APPROVED', 'Cleared',                                   (SELECT id FROM users WHERE username = 'accountant_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (143, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0010' LIMIT 1) LIMIT 1), 'LABORATORY',      'APPROVED', 'Cleared',                                   (SELECT id FROM users WHERE username = 'faculty_tan' LIMIT 1),    DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 DAY)),
    (144, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0010' LIMIT 1) LIMIT 1), 'STUDENT_AFFAIRS', 'PENDING',  NULL,                                        NULL, NULL),
    (145, (SELECT id FROM clearance_requests WHERE student_profile_id = (SELECT id FROM student_profiles WHERE student_number = '2026-0010' LIMIT 1) LIMIT 1), 'DEAN',            'PENDING',  NULL,                                        NULL, NULL)
AS new_so ON DUPLICATE KEY UPDATE signoff_status = new_so.signoff_status;

-- Graduation Applications with CHED Special Order References (Dynamic Application Date)
INSERT INTO graduation_applications (
    id, student_profile_id, curriculum_id, term_id, application_date, 
    degree_audit_status, total_units_completed, cumulative_gpa, 
    honors_status, special_order_number
) VALUES 
    (10, 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1), 
     (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), 
     (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY), 
     'QUALIFIED', 126.00, 1.40, 'MAGNA_CUM_LAUDE', 'SO-2026-CHED-0091'),
    
    (11, 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0006' LIMIT 1), 
     (SELECT id FROM curricula WHERE code = 'BSBA-FM-2026' LIMIT 1), 
     (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY), 
     'QUALIFIED', 120.00, 1.55, 'CUM_LAUDE', 'SO-2026-CHED-0092'),
    
    (12, 
     (SELECT id FROM student_profiles WHERE student_number = '2026-0010' LIMIT 1), 
     (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), 
     (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1), 
     DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY), 
     'PENDING', 115.00, 1.80, 'NONE', NULL)
AS new_grad ON DUPLICATE KEY UPDATE degree_audit_status = new_grad.degree_audit_status;

-- -----------------------------------------------------------------------------
-- 12. STATUTORY PHILIPPINE EQUITY TARGET PROFILING
-- Covers: RA 10931, RA 8371 (IPRA), RA 7277 (PWD), RA 11861 (Solo Parents),
--         RA 11310 (4Ps), DOH GIDA, and DA RSBSA Farmer/Fisherfolk.
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
    -- Student 10 (Juan Dela Cruz): 4Ps / TES / Listahanan / Bottom 40% / First-Gen
    (10, (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1), 
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
     'VERIFIED', (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 22 DAY), 'Verified against DSWD Listahanan & UniFAST master list'),

    -- Student 11 (Maria Santos): Raised by Solo Parent / Low Income
    (11, (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1), 
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
     'VERIFIED', (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 22 DAY), 'Verified via LGU Social Welfare Office Solo Parent ID'),

    -- Student 12 (Paolo Reyes): PWD Visual / GIDA / Farmer-Fisherfolk
    (12, (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1), 
     FALSE, NULL, FALSE, FALSE, NULL, 
     FALSE, NULL, NULL, 
     TRUE, 'PWD-NEGROS-8821', 'VISUAL', 
     FALSE, FALSE, NULL, 
     FALSE, 
     TRUE, 'Brgy. Katilingban (GIDA Island Community)', 
     TRUE, 'RSBSA-06-45-0912', 
     FALSE, NULL, 
     TRUE, 'LOW_INCOME_10K_TO_20K', 
     TRUE, 
     'VERIFIED', (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 22 DAY), 'Verified via PWD ID, RSBSA Registry & Barangay Certificate'),

    -- Student 13 (Angela Dalisay): Farmer/Fisherfolk Child
    (13, (SELECT id FROM student_profiles WHERE student_number = '2026-0004' LIMIT 1), 
     FALSE, NULL, FALSE, FALSE, NULL, 
     FALSE, NULL, NULL, 
     FALSE, NULL, NULL, 
     FALSE, FALSE, NULL, 
     FALSE, 
     FALSE, NULL, 
     TRUE, 'RSBSA-06-32-1144', 
     FALSE, NULL, 
     TRUE, 'LOW_INCOME_10K_TO_20K', 
     TRUE, 
     'VERIFIED', (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 22 DAY), 'Verified against Department of Agriculture RSBSA records'),

    -- Student 15 (Fatima Hassan): Indigenous People (Panay Bukidnon)
    (14, (SELECT id FROM student_profiles WHERE student_number = '2026-0006' LIMIT 1), 
     FALSE, NULL, FALSE, TRUE, 'TES-2026-04419', 
     TRUE, 'Panay Bukidnon', 'NCIP-RO6-2023-0988', 
     FALSE, NULL, NULL, 
     FALSE, FALSE, NULL, 
     FALSE, 
     TRUE, 'Brgy. Garangan, Tapaz (GIDA Mountain Zone)', 
     FALSE, NULL, 
     FALSE, NULL, 
     TRUE, 'POOR_BELOW_10K', 
     TRUE, 
     'VERIFIED', (SELECT id FROM users WHERE username = 'registrar_head' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 22 DAY), 'Verified via NCIP Certificate of Confirmation')
AS new_eq ON DUPLICATE KEY UPDATE verification_status = new_eq.verification_status;

-- -----------------------------------------------------------------------------
-- 13. DIGITAL TWIN TELEMETRY, GEOFENCED QR ATTENDANCE & ML RISK SCORES
-- CHMSU Talisay Geofence Coordinates: 10.69750000 N, 122.96440000 E (Radius: 50m)
-- Live session: QR code actively valid for the next 2 hours
-- -----------------------------------------------------------------------------
INSERT INTO attendance_sessions (id, section_schedule_id, session_date, qr_seed, qr_expires_at, latitude, longitude, allowed_radius_meters)
VALUES 
    (10, 
     (SELECT id FROM class_schedules WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-3A' LIMIT 1) LIMIT 1), 
     CURRENT_DATE, 
     'SEED-BSCS3A-LIVE-001', 
     DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 2 HOUR), 
     10.69750000, 122.96440000, 50),
    
    (11, 
     (SELECT id FROM class_schedules WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-2A' LIMIT 1) LIMIT 1), 
     CURRENT_DATE, 
     'SEED-BSIT2A-LIVE-002', 
     DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 2 HOUR), 
     10.69750000, 122.96440000, 50)
AS new_att_sess ON DUPLICATE KEY UPDATE 
    session_date = new_att_sess.session_date,
    qr_seed = new_att_sess.qr_seed,
    qr_expires_at = new_att_sess.qr_expires_at;

INSERT INTO attendance_records (id, attendance_session_id, student_profile_id, scanned_at, attendance_status, device_fingerprint, verified_latitude, verified_longitude)
VALUES 
    (10, 10, (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 15 MINUTE), 'PRESENT', 'FP-IPHONE-STUDENT-A', 10.69751000, 122.96441000),
    (11, 10, (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 MINUTE),  'LATE',    'FP-ANDROID-STUDENT-B', 10.69758000, 122.96445000),
    (12, 10, (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1), DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 20 MINUTE), 'PRESENT', 'FP-LAPTOP-STUDENT-C', 10.69749000, 122.96439000)
AS new_att_rec ON DUPLICATE KEY UPDATE attendance_status = new_att_rec.attendance_status;

-- Student Risk Scores (Dynamic Timestamp & Full Enum Coverage: LOW, MODERATE, HIGH, CRITICAL)
INSERT INTO student_risk_scores (
    id, student_profile_id, evaluated_at, 
    academic_risk_score, attendance_risk_score, socioeconomic_risk_score, 
    composite_risk_level, predicted_dropout_probability, recommended_interventions
) VALUES 
    (10, (SELECT id FROM student_profiles WHERE student_number = '2026-0001' LIMIT 1), 
     CURRENT_TIMESTAMP, 0.05, 0.05, 0.35, 'LOW', 0.0350, 'Maintain standard academic advising and UniFAST stipend monitoring.'),
    
    (11, (SELECT id FROM student_profiles WHERE student_number = '2026-0002' LIMIT 1), 
     CURRENT_TIMESTAMP, 0.65, 0.70, 0.80, 'HIGH', 0.7150, 'Urgent counseling referral for financial difficulty and academic tutoring intervention in CS301.'),
    
    (12, (SELECT id FROM student_profiles WHERE student_number = '2026-0003' LIMIT 1), 
     CURRENT_TIMESTAMP, 0.10, 0.05, 0.30, 'LOW', 0.0400, 'Proceed with Latin Honors degree audit verification for Graduation.'),
    
    (13, (SELECT id FROM student_profiles WHERE student_number = '2026-0008' LIMIT 1), 
     CURRENT_TIMESTAMP, 0.45, 0.40, 0.55, 'MODERATE', 0.4600, 'Working student schedule conflict: Recommend evening section adjustment and academic assistance.'),
    
    (14, (SELECT id FROM student_profiles WHERE student_number = '2026-0015' LIMIT 1), 
     CURRENT_TIMESTAMP, 0.90, 0.85, 0.75, 'CRITICAL', 0.8850, 'Maximum Residency Rule cap reached: Priority academic retention board hearing required.')
AS new_risk ON DUPLICATE KEY UPDATE 
    evaluated_at = new_risk.evaluated_at,
    composite_risk_level = new_risk.composite_risk_level;

-- -----------------------------------------------------------------------------
-- 14. LMS LTI 1.3 ADVANTAGE INTEROPERABILITY DEPLOYMENTS
-- -----------------------------------------------------------------------------
INSERT INTO lti_deployments (id, platform_name, client_id, deployment_id, oidc_auth_url, access_token_url, jwks_url, is_active)
VALUES 
    (10, 'Canvas LMS - CHMSU Portal', 'canvas-client-id-chmsu-2026', 'dep-001-canvas-main', 'https://canvas.chmsu.edu.ph/api/lti/authorize', 'https://canvas.chmsu.edu.ph/login/oauth2/token', 'https://canvas.chmsu.edu.ph/api/lti/security/jwks', TRUE),
    (11, 'Moodle OpenLMS - CHMSU', 'moodle-client-id-chmsu-2026', 'dep-002-moodle-ft', 'https://moodle.chmsu.edu.ph/mod/lti/auth.php', 'https://moodle.chmsu.edu.ph/mod/lti/token.php', 'https://moodle.chmsu.edu.ph/mod/lti/certs.php', TRUE)
AS new_lti ON DUPLICATE KEY UPDATE platform_name = new_lti.platform_name;

INSERT INTO lti_user_mappings (id, lti_deployment_id, user_id, sub_claim)
VALUES 
    (10, (SELECT id FROM lti_deployments WHERE deployment_id = 'dep-001-canvas-main' LIMIT 1), (SELECT id FROM users WHERE username = 'faculty_smith' LIMIT 1), 'canvas-sub-prof-smith'),
    (11, (SELECT id FROM lti_deployments WHERE deployment_id = 'dep-001-canvas-main' LIMIT 1), (SELECT id FROM users WHERE username = 'faculty_tan' LIMIT 1),   'canvas-sub-prof-tan'),
    (12, (SELECT id FROM lti_deployments WHERE deployment_id = 'dep-001-canvas-main' LIMIT 1), (SELECT id FROM users WHERE username = 'student_a' LIMIT 1),     'canvas-sub-student-juan'),
    (13, (SELECT id FROM lti_deployments WHERE deployment_id = 'dep-001-canvas-main' LIMIT 1), (SELECT id FROM users WHERE username = 'student_b' LIMIT 1),     'canvas-sub-student-maria')
AS new_lti_map ON DUPLICATE KEY UPDATE sub_claim = new_lti_map.sub_claim;

INSERT INTO lti_oidc_states (id, state, nonce, client_id, deployment_id, target_link_uri, expires_at)
VALUES (
    10, 
    'state-active-session-001', 
    'nonce-token-chmsu-991', 
    'canvas-client-id-chmsu-2026', 
    'dep-001-canvas-main', 
    'https://sdt.chmsu.edu.ph/lti/launch', 
    DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
)
AS new_state ON DUPLICATE KEY UPDATE nonce = new_state.nonce;

INSERT INTO lti_lineitems (id, deployment_id, section_id, lineitem_url, label, maximum_score, resource_id, tag)
VALUES (
    10, 
    (SELECT id FROM lti_deployments WHERE deployment_id = 'dep-001-canvas-main' LIMIT 1), 
    (SELECT id FROM class_sections WHERE section_code = 'BSCS-3A' LIMIT 1), 
    'https://canvas.chmsu.edu.ph/api/lti/courses/10/line_items/101', 
    'Midterm Machine Problem Score Sync', 
    100.00, 
    'res-mp1-bscs3a', 
    'ASSIGNMENT_AGS'
)
AS new_li ON DUPLICATE KEY UPDATE maximum_score = new_li.maximum_score;

SET FOREIGN_KEY_CHECKS = 1;
