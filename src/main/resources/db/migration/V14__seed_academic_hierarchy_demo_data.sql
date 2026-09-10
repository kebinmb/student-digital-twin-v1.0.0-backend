-- V14__seed_academic_hierarchy_demo_data.sql
-- Subsystem: Academic Hierarchy, Scoping Personas, Curriculum, Section, and Dynamic Class Record Fixtures
-- Standard Compliance: Strict Multi-College & Multi-Program Scoping Isolation (DEAN, CHAIRPERSON, FACULTY, STUDENT)

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. Ensure Colleges (CCS and COE)
-- -----------------------------------------------------------------------------
INSERT INTO departments (campus_id, code, name, type, parent_department_id, is_active)
SELECT (SELECT id FROM campuses WHERE code IN ('ALIIJIS', 'ALIJIS') LIMIT 1), 'CCS', 'College of Computer Studies', 'COLLEGE', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE code = 'CCS');

INSERT INTO departments (campus_id, code, name, type, parent_department_id, is_active)
SELECT (SELECT id FROM campuses WHERE code IN ('TALISAY', 'MAIN') LIMIT 1), 'COE', 'College of Engineering', 'COLLEGE', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE code = 'COE');

UPDATE departments SET type = 'COLLEGE', is_active = TRUE WHERE code IN ('CCS', 'COE');

-- -----------------------------------------------------------------------------
-- 2. Ensure Academic Programs (BSIT, BSCS, BSCE, BSME)
-- -----------------------------------------------------------------------------
-- Program BSIT under CCS
INSERT INTO programs (department_id, college_id, code, name, major, degree_level, ched_cmo_reference, government_permit, total_units_required, is_active)
SELECT (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1),
       (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1),
       'BSIT', 'Bachelor of Science in Information Technology', 'Network, Security & Web Systems', 'UNDERGRADUATE', 'CMO No. 25, Series 2015', 'PSG for Information Technology', 146, TRUE
WHERE NOT EXISTS (SELECT 1 FROM programs WHERE code = 'BSIT');

UPDATE programs
SET college_id = (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1)
WHERE code = 'BSIT';

-- Program BSCS under CCS
INSERT INTO programs (department_id, college_id, code, name, major, degree_level, ched_cmo_reference, government_permit, total_units_required, is_active)
SELECT (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1),
       (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1),
       'BSCS', 'Bachelor of Science in Computer Science', 'Algorithms & Intelligent Systems', 'UNDERGRADUATE', 'CMO No. 25, Series 2015', 'PSG for Computer Science', 146, TRUE
WHERE NOT EXISTS (SELECT 1 FROM programs WHERE code = 'BSCS');

UPDATE programs
SET college_id = (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1)
WHERE code = 'BSCS';

-- Program BSCE under COE
INSERT INTO programs (department_id, college_id, code, name, major, degree_level, ched_cmo_reference, government_permit, total_units_required, is_active)
SELECT (SELECT id FROM departments WHERE code = 'COE' LIMIT 1),
       (SELECT id FROM departments WHERE code = 'COE' LIMIT 1),
       'BSCE', 'Bachelor of Science in Civil Engineering', 'Structural & Water Resources', 'UNDERGRADUATE', 'CMO No. 92, Series 2017', 'PSG for Civil Engineering', 160, TRUE
WHERE NOT EXISTS (SELECT 1 FROM programs WHERE code = 'BSCE');

UPDATE programs
SET college_id = (SELECT id FROM departments WHERE code = 'COE' LIMIT 1)
WHERE code = 'BSCE';

-- Program BSME under COE
INSERT INTO programs (department_id, college_id, code, name, major, degree_level, ched_cmo_reference, government_permit, total_units_required, is_active)
SELECT (SELECT id FROM departments WHERE code = 'COE' LIMIT 1),
       (SELECT id FROM departments WHERE code = 'COE' LIMIT 1),
       'BSME', 'Bachelor of Science in Mechanical Engineering', 'Mechanical & Thermal Systems', 'UNDERGRADUATE', 'CMO No. 97, Series 2017', 'PSG for Mechanical Engineering', 160, TRUE
WHERE NOT EXISTS (SELECT 1 FROM programs WHERE code = 'BSME');

UPDATE programs
SET college_id = (SELECT id FROM departments WHERE code = 'COE' LIMIT 1)
WHERE code = 'BSME';

-- -----------------------------------------------------------------------------
-- 3. Ensure Curricula (BSIT-2026, BSCS-2026, BSCE-2026, BSME-2026)
-- -----------------------------------------------------------------------------
INSERT INTO curricula (program_id, code, name, status, version_number, effective_academic_year, is_active)
SELECT (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'BSIT-2026', 'BSIT Curriculum 2026-2030', 'ACTIVE', 1, '2026-2027', TRUE
WHERE NOT EXISTS (SELECT 1 FROM curricula WHERE code = 'BSIT-2026');

INSERT INTO curricula (program_id, code, name, status, version_number, effective_academic_year, is_active)
SELECT (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), 'BSCS-2026', 'BSCS Curriculum 2026-2030', 'ACTIVE', 1, '2026-2027', TRUE
WHERE NOT EXISTS (SELECT 1 FROM curricula WHERE code = 'BSCS-2026');

INSERT INTO curricula (program_id, code, name, status, version_number, effective_academic_year, is_active)
SELECT (SELECT id FROM programs WHERE code = 'BSCE' LIMIT 1), 'BSCE-2026', 'BSCE Curriculum 2026-2030', 'ACTIVE', 1, '2026-2027', TRUE
WHERE NOT EXISTS (SELECT 1 FROM curricula WHERE code = 'BSCE-2026');

INSERT INTO curricula (program_id, code, name, status, version_number, effective_academic_year, is_active)
SELECT (SELECT id FROM programs WHERE code = 'BSME' LIMIT 1), 'BSME-2026', 'BSME Curriculum 2026-2030', 'ACTIVE', 1, '2026-2027', TRUE
WHERE NOT EXISTS (SELECT 1 FROM curricula WHERE code = 'BSME-2026');

UPDATE curricula SET status = 'ACTIVE', is_active = TRUE WHERE code IN ('BSIT-2026', 'BSCS-2026', 'BSCE-2026', 'BSME-2026');

-- -----------------------------------------------------------------------------
-- 4. Ensure Prescribed Courses & Curriculum Mappings
-- -----------------------------------------------------------------------------
INSERT INTO courses (code, title, lecture_units, lab_units, credit_units, contact_hours_lec, contact_hours_lab, category, description, is_active)
VALUES
('CS-101', 'Discrete Structures 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Computer Science Core | Discrete Structures, Sets, and Logic', TRUE),
('ME-101', 'Thermodynamics 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Mechanical Engineering Core | Thermodynamics Principles & Work-Heat Systems', TRUE)
ON DUPLICATE KEY UPDATE title = VALUES(title), credit_units = VALUES(credit_units);

-- Map courses for BSCS-2026
INSERT INTO curriculum_courses (curriculum_id, course_id, year_level, sequence_order, category, semester)
SELECT (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1),
       (SELECT id FROM courses WHERE code = 'GE-USELF' LIMIT 1), 1, 1, 'GEN_ED', '1ST_SEM'
WHERE NOT EXISTS (SELECT 1 FROM curriculum_courses WHERE curriculum_id = (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1) AND course_id = (SELECT id FROM courses WHERE code = 'GE-USELF' LIMIT 1));

INSERT INTO curriculum_courses (curriculum_id, course_id, year_level, sequence_order, category, semester)
SELECT (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1),
       (SELECT id FROM courses WHERE code = 'CC-101' LIMIT 1), 1, 2, 'PROFESSIONAL_MAJOR', '1ST_SEM'
WHERE NOT EXISTS (SELECT 1 FROM curriculum_courses WHERE curriculum_id = (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1) AND course_id = (SELECT id FROM courses WHERE code = 'CC-101' LIMIT 1));

INSERT INTO curriculum_courses (curriculum_id, course_id, year_level, sequence_order, category, semester)
SELECT (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1),
       (SELECT id FROM courses WHERE code = 'CS-101' LIMIT 1), 1, 3, 'PROFESSIONAL_MAJOR', '1ST_SEM'
WHERE NOT EXISTS (SELECT 1 FROM curriculum_courses WHERE curriculum_id = (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1) AND course_id = (SELECT id FROM courses WHERE code = 'CS-101' LIMIT 1));

INSERT INTO curriculum_courses (curriculum_id, course_id, year_level, sequence_order, category, semester)
SELECT (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1),
       (SELECT id FROM courses WHERE code = 'CC-102' LIMIT 1), 1, 4, 'PROFESSIONAL_MAJOR', '2ND_SEM'
WHERE NOT EXISTS (SELECT 1 FROM curriculum_courses WHERE curriculum_id = (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1) AND course_id = (SELECT id FROM courses WHERE code = 'CC-102' LIMIT 1));

-- Map courses for BSME-2026
INSERT INTO curriculum_courses (curriculum_id, course_id, year_level, sequence_order, category, semester)
SELECT (SELECT id FROM curricula WHERE code = 'BSME-2026' LIMIT 1),
       (SELECT id FROM courses WHERE code = 'GE-USELF' LIMIT 1), 1, 1, 'GEN_ED', '1ST_SEM'
WHERE NOT EXISTS (SELECT 1 FROM curriculum_courses WHERE curriculum_id = (SELECT id FROM curricula WHERE code = 'BSME-2026' LIMIT 1) AND course_id = (SELECT id FROM courses WHERE code = 'GE-USELF' LIMIT 1));

INSERT INTO curriculum_courses (curriculum_id, course_id, year_level, sequence_order, category, semester)
SELECT (SELECT id FROM curricula WHERE code = 'BSME-2026' LIMIT 1),
       (SELECT id FROM courses WHERE code = 'MATH-CALC1' LIMIT 1), 1, 2, 'PROFESSIONAL_MAJOR', '1ST_SEM'
WHERE NOT EXISTS (SELECT 1 FROM curriculum_courses WHERE curriculum_id = (SELECT id FROM curricula WHERE code = 'BSME-2026' LIMIT 1) AND course_id = (SELECT id FROM courses WHERE code = 'MATH-CALC1' LIMIT 1));

INSERT INTO curriculum_courses (curriculum_id, course_id, year_level, sequence_order, category, semester)
SELECT (SELECT id FROM curricula WHERE code = 'BSME-2026' LIMIT 1),
       (SELECT id FROM courses WHERE code = 'ME-101' LIMIT 1), 1, 3, 'PROFESSIONAL_MAJOR', '1ST_SEM'
WHERE NOT EXISTS (SELECT 1 FROM curriculum_courses WHERE curriculum_id = (SELECT id FROM curricula WHERE code = 'BSME-2026' LIMIT 1) AND course_id = (SELECT id FROM courses WHERE code = 'ME-101' LIMIT 1));

-- -----------------------------------------------------------------------------
-- 5. Ensure Academic Years & Historical Term
-- -----------------------------------------------------------------------------
INSERT INTO academic_years (code, start_date, end_date, is_current)
SELECT 'AY 2025-2026', '2025-08-11', '2026-07-17', FALSE
WHERE NOT EXISTS (SELECT 1 FROM academic_years WHERE code = 'AY 2025-2026');

INSERT INTO academic_years (code, start_date, end_date, is_current)
SELECT 'AY 2026-2027', '2026-08-10', '2027-07-16', TRUE
WHERE NOT EXISTS (SELECT 1 FROM academic_years WHERE code = 'AY 2026-2027');

INSERT INTO terms (academic_year_id, term_type, term_name, start_date, end_date, enrollment_open, grading_open, add_drop_open, is_active)
SELECT (SELECT id FROM academic_years WHERE code = 'AY 2025-2026' LIMIT 1),
       'SECOND_SEM', '2nd Semester AY 2025-2026', '2026-01-12', '2026-05-29', FALSE, FALSE, FALSE, FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM terms 
    WHERE academic_year_id = (SELECT id FROM academic_years WHERE code = 'AY 2025-2026' LIMIT 1) 
      AND term_type = 'SECOND_SEM'
);

-- -----------------------------------------------------------------------------
-- 6. Ensure Lecture & Laboratory Rooms
-- -----------------------------------------------------------------------------
INSERT INTO rooms (campus_id, code, name, building, floor, capacity, room_type, is_active)
VALUES
((SELECT id FROM campuses WHERE code IN ('TALISAY', 'MAIN') LIMIT 1), 'TAL-IT-LAB1', 'Computer Laboratory 1', 'Engineering & Tech Building', 2, 40, 'LABORATORY', TRUE),
((SELECT id FROM campuses WHERE code IN ('TALISAY', 'MAIN') LIMIT 1), 'TAL-IT-LAB2', 'Computer Laboratory 2', 'Engineering & Tech Building', 2, 40, 'LABORATORY', TRUE),
((SELECT id FROM campuses WHERE code IN ('TALISAY', 'MAIN') LIMIT 1), 'TAL-ENG-301', 'Engineering Lecture Hall 301', 'Engineering & Tech Building', 3, 50, 'LECTURE', TRUE),
((SELECT id FROM campuses WHERE code IN ('TALISAY', 'MAIN') LIMIT 1), 'TAL-ENG-302', 'Engineering Lecture Hall 302', 'Engineering & Tech Building', 3, 50, 'LECTURE', TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), capacity = VALUES(capacity);

-- -----------------------------------------------------------------------------
-- 7. Insert Demo Users with Strict Scoping Links (Password: Password123!)
-- -----------------------------------------------------------------------------
INSERT INTO users (username, email, password, enabled, college_id, program_id, created_at)
VALUES
('admin.demo', 'admin.demo@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, NULL, NULL, NOW()),
('registrar.demo', 'registrar.demo@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, NULL, NULL, NOW()),
('dean.ccs', 'dean.ccs@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), NULL, NOW()),
('dean.coe', 'dean.coe@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), NULL, NOW()),
('chair.bsit', 'chair.bsit@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), NOW()),
('faculty.bsit', 'faculty.bsit@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), NOW()),
('student.bsit.cleared', 'student.bsit.cleared@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), NOW()),
('student.bsit.blocked', 'student.bsit.blocked@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), NOW()),
('chair.bscs', 'chair.bscs@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), NOW()),
('faculty.bscs', 'faculty.bscs@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), NOW()),
('student.bscs.demo', 'student.bscs.demo@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), NOW()),
('chair.bsce', 'chair.bsce@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSCE' LIMIT 1), NOW()),
('faculty.coe', 'faculty.coe@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSCE' LIMIT 1), NOW()),
('student.bsce.demo', 'student.bsce.demo@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSCE' LIMIT 1), NOW()),
('chair.bsme', 'chair.bsme@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSME' LIMIT 1), NOW()),
('faculty.bsme', 'faculty.bsme@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSME' LIMIT 1), NOW())
ON DUPLICATE KEY UPDATE 
    college_id = VALUES(college_id), 
    program_id = VALUES(program_id), 
    password = VALUES(password), 
    enabled = TRUE;

-- -----------------------------------------------------------------------------
-- 8. Assign Role Enums
-- -----------------------------------------------------------------------------
INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE username = 'admin.demo'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'REGISTRAR' FROM users WHERE username = 'registrar.demo'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'DEAN' FROM users WHERE username = 'dean.ccs'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'FACULTY' FROM users WHERE username = 'dean.ccs'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'DEAN' FROM users WHERE username = 'dean.coe'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'FACULTY' FROM users WHERE username = 'dean.coe'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'CHAIRPERSON' FROM users WHERE username = 'chair.bsit'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'FACULTY' FROM users WHERE username = 'chair.bsit'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'FACULTY' FROM users WHERE username = 'faculty.bsit'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'STUDENT' FROM users WHERE username = 'student.bsit.cleared'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'STUDENT' FROM users WHERE username = 'student.bsit.blocked'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'CHAIRPERSON' FROM users WHERE username = 'chair.bscs'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'FACULTY' FROM users WHERE username = 'chair.bscs'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'FACULTY' FROM users WHERE username = 'faculty.bscs'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'STUDENT' FROM users WHERE username = 'student.bscs.demo'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'CHAIRPERSON' FROM users WHERE username = 'chair.bsce'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'FACULTY' FROM users WHERE username = 'chair.bsce'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'FACULTY' FROM users WHERE username = 'faculty.coe'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'STUDENT' FROM users WHERE username = 'student.bsce.demo'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'CHAIRPERSON' FROM users WHERE username = 'chair.bsme'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'FACULTY' FROM users WHERE username = 'chair.bsme'
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'FACULTY' FROM users WHERE username = 'faculty.bsme'
ON DUPLICATE KEY UPDATE role = VALUES(role);

-- -----------------------------------------------------------------------------
-- 9. Update Department Deans and Program Chairpersons
-- -----------------------------------------------------------------------------
UPDATE departments SET dean_user_id = (SELECT id FROM users WHERE username = 'dean.ccs' LIMIT 1) WHERE code = 'CCS';
UPDATE departments SET dean_user_id = (SELECT id FROM users WHERE username = 'dean.coe' LIMIT 1) WHERE code = 'COE';

UPDATE programs SET chairperson_user_id = (SELECT id FROM users WHERE username = 'chair.bsit' LIMIT 1) WHERE code = 'BSIT';
UPDATE programs SET chairperson_user_id = (SELECT id FROM users WHERE username = 'chair.bscs' LIMIT 1) WHERE code = 'BSCS';
UPDATE programs SET chairperson_user_id = (SELECT id FROM users WHERE username = 'chair.bsce' LIMIT 1) WHERE code = 'BSCE';
UPDATE programs SET chairperson_user_id = (SELECT id FROM users WHERE username = 'chair.bsme' LIMIT 1) WHERE code = 'BSME';

-- -----------------------------------------------------------------------------
-- 10. Faculty Profiles
-- -----------------------------------------------------------------------------
INSERT INTO faculty_profiles (user_id, college_id, program_id, faculty_id_number, highest_degree, academic_rank, prc_license_no, employment_status, is_tenured)
VALUES
((SELECT id FROM users WHERE username = 'dean.ccs' LIMIT 1), (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), NULL, 'FAC-CCS-DEAN', 'DOCTORATE', 'PROFESSOR_I', 'PRC-0011111', 'FULL_TIME', TRUE),
((SELECT id FROM users WHERE username = 'dean.coe' LIMIT 1), (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), NULL, 'FAC-COE-DEAN', 'DOCTORATE', 'PROFESSOR_I', 'PRC-0022222', 'FULL_TIME', TRUE),
((SELECT id FROM users WHERE username = 'chair.bsit' LIMIT 1), (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'FAC-BSIT-CHAIR', 'MASTERS', 'ASSOCIATE_PROFESSOR_I', 'PRC-0033333', 'FULL_TIME', TRUE),
((SELECT id FROM users WHERE username = 'faculty.bsit' LIMIT 1), (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'FAC-BSIT-001', 'MASTERS', 'ASSISTANT_PROFESSOR_I', 'PRC-0044444', 'FULL_TIME', TRUE),
((SELECT id FROM users WHERE username = 'chair.bscs' LIMIT 1), (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), 'FAC-BSCS-CHAIR', 'MASTERS', 'ASSOCIATE_PROFESSOR_I', 'PRC-0055555', 'FULL_TIME', TRUE),
((SELECT id FROM users WHERE username = 'faculty.bscs' LIMIT 1), (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), 'FAC-BSCS-001', 'MASTERS', 'ASSISTANT_PROFESSOR_I', 'PRC-0066666', 'FULL_TIME', TRUE),
((SELECT id FROM users WHERE username = 'chair.bsce' LIMIT 1), (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSCE' LIMIT 1), 'FAC-BSCE-CHAIR', 'MASTERS', 'ASSOCIATE_PROFESSOR_I', 'PRC-0077777', 'FULL_TIME', TRUE),
((SELECT id FROM users WHERE username = 'faculty.coe' LIMIT 1), (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSCE' LIMIT 1), 'FAC-BSCE-001', 'MASTERS', 'ASSISTANT_PROFESSOR_I', 'PRC-0088888', 'FULL_TIME', TRUE),
((SELECT id FROM users WHERE username = 'chair.bsme' LIMIT 1), (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSME' LIMIT 1), 'FAC-BSME-CHAIR', 'MASTERS', 'ASSOCIATE_PROFESSOR_I', 'PRC-0099999', 'FULL_TIME', TRUE),
((SELECT id FROM users WHERE username = 'faculty.bsme' LIMIT 1), (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSME' LIMIT 1), 'FAC-BSME-001', 'MASTERS', 'ASSISTANT_PROFESSOR_I', 'PRC-0100000', 'FULL_TIME', TRUE)
ON DUPLICATE KEY UPDATE
    college_id = VALUES(college_id),
    program_id = VALUES(program_id),
    faculty_id_number = VALUES(faculty_id_number),
    academic_rank = VALUES(academic_rank);

-- -----------------------------------------------------------------------------
-- 11. Student Profiles with Clearance Flags & Historical Transcript
-- -----------------------------------------------------------------------------
INSERT INTO student_profiles (user_id, student_number, program_id, curriculum_id, year_level, enrollment_status, student_classification, is_graduating, total_units_earned, cumulative_gpa, financial_clearance, departmental_clearance)
VALUES
((SELECT id FROM users WHERE username = 'student.bsit.cleared' LIMIT 1), '2026-IT-1001', (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), 1, 'REGULAR', 'CONTINUING', FALSE, 23.00, 1.45, 'CLEARED', 'CLEARED'),
((SELECT id FROM users WHERE username = 'student.bsit.blocked' LIMIT 1), '2026-IT-1002', (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), 2, 'REGULAR', 'CONTINUING', FALSE, 26.00, 2.20, 'PENDING', 'BLOCKED'),
((SELECT id FROM users WHERE username = 'student.bscs.demo' LIMIT 1), '2026-CS-1001', (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1), 1, 'REGULAR', 'CONTINUING', FALSE, 23.00, 1.50, 'CLEARED', 'CLEARED'),
((SELECT id FROM users WHERE username = 'student.bsce.demo' LIMIT 1), '2026-CE-1001', (SELECT id FROM programs WHERE code = 'BSCE' LIMIT 1), (SELECT id FROM curricula WHERE code = 'BSCE-2026' LIMIT 1), 1, 'REGULAR', 'CONTINUING', FALSE, 24.00, 1.60, 'CLEARED', 'CLEARED')
ON DUPLICATE KEY UPDATE
    program_id = VALUES(program_id),
    curriculum_id = VALUES(curriculum_id),
    student_number = VALUES(student_number),
    financial_clearance = VALUES(financial_clearance),
    departmental_clearance = VALUES(departmental_clearance),
    cumulative_gpa = VALUES(cumulative_gpa);

-- Historical Passed Prerequisite Grades for student.bsit.cleared
INSERT INTO student_course_grades (student_id, course_id, numerical_grade, completion_status, is_credited)
SELECT (SELECT id FROM student_profiles WHERE student_number = '2026-IT-1001' LIMIT 1),
       c.id, 1.50, 'PASSED', TRUE
FROM courses c
WHERE c.code IN ('GE-USELF', 'GE-RPH', 'GE-PC', 'GE-MMW', 'PE-1', 'NSTP-1')
  AND NOT EXISTS (
      SELECT 1 FROM student_course_grades scg 
      WHERE scg.student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-IT-1001' LIMIT 1) 
        AND scg.course_id = c.id
  );

-- -----------------------------------------------------------------------------
-- 12. Class Sections
-- -----------------------------------------------------------------------------
INSERT INTO class_sections (term_id, curriculum_id, course_id, section_code, max_capacity, enrolled_count, status, grade_status, primary_instructor_id)
VALUES
(
    (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1),
    (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1),
    (SELECT id FROM courses WHERE code = 'CC-101' LIMIT 1),
    'BSIT-1A', 40, 1, 'OPEN', 'DRAFT',
    (SELECT id FROM users WHERE username = 'faculty.bsit' LIMIT 1)
),
(
    (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1),
    (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1),
    (SELECT id FROM courses WHERE code = 'CC-102' LIMIT 1),
    'BSIT-2A', 40, 1, 'OPEN', 'DRAFT',
    (SELECT id FROM users WHERE username = 'faculty.bsit' LIMIT 1)
),
(
    (SELECT id FROM terms WHERE term_name = '2nd Semester AY 2025-2026' LIMIT 1),
    (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1),
    (SELECT id FROM courses WHERE code = 'CC-101' LIMIT 1),
    'BSIT-3A', 40, 1, 'CLOSED', 'SEALED',
    (SELECT id FROM users WHERE username = 'faculty.bsit' LIMIT 1)
),
(
    (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1),
    (SELECT id FROM curricula WHERE code = 'BSCS-2026' LIMIT 1),
    (SELECT id FROM courses WHERE code = 'CC-101' LIMIT 1),
    'BSCS-1A', 40, 1, 'OPEN', 'DRAFT',
    (SELECT id FROM users WHERE username = 'faculty.bscs' LIMIT 1)
),
(
    (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1),
    (SELECT id FROM curricula WHERE code = 'BSCE-2026' LIMIT 1),
    (SELECT id FROM courses WHERE code = 'MATH-CALC1' LIMIT 1),
    'BSCE-1A', 40, 1, 'OPEN', 'DRAFT',
    (SELECT id FROM users WHERE username = 'faculty.coe' LIMIT 1)
),
(
    (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1),
    (SELECT id FROM curricula WHERE code = 'BSME-2026' LIMIT 1),
    (SELECT id FROM courses WHERE code = 'MATH-CALC1' LIMIT 1),
    'BSME-1A', 40, 0, 'OPEN', 'DRAFT',
    (SELECT id FROM users WHERE username = 'faculty.bsme' LIMIT 1)
)
ON DUPLICATE KEY UPDATE
    primary_instructor_id = VALUES(primary_instructor_id),
    status = VALUES(status),
    grade_status = VALUES(grade_status);

-- -----------------------------------------------------------------------------
-- 13. Class Schedules
-- -----------------------------------------------------------------------------
DELETE cs FROM class_schedules cs
JOIN class_sections s ON cs.section_id = s.id
WHERE s.section_code IN ('BSIT-1A', 'BSIT-2A', 'BSIT-3A', 'BSCS-1A', 'BSCE-1A', 'BSME-1A');

INSERT INTO class_schedules (section_id, room_id, instructor_user_id, day_of_week, start_time, end_time, schedule_type)
VALUES
(
    (SELECT id FROM class_sections WHERE section_code = 'BSIT-1A' LIMIT 1),
    (SELECT id FROM rooms WHERE code = 'TAL-IT-LAB1' LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.bsit' LIMIT 1),
    'MONDAY', '08:00:00', '10:00:00', 'LABORATORY'
),
(
    (SELECT id FROM class_sections WHERE section_code = 'BSIT-1A' LIMIT 1),
    (SELECT id FROM rooms WHERE code = 'TAL-IT-LAB1' LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.bsit' LIMIT 1),
    'WEDNESDAY', '08:00:00', '09:00:00', 'LECTURE'
),
(
    (SELECT id FROM class_sections WHERE section_code = 'BSIT-2A' LIMIT 1),
    (SELECT id FROM rooms WHERE code = 'TAL-IT-LAB1' LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.bsit' LIMIT 1),
    'TUESDAY', '09:00:00', '11:00:00', 'LABORATORY'
),
(
    (SELECT id FROM class_sections WHERE section_code = 'BSIT-2A' LIMIT 1),
    (SELECT id FROM rooms WHERE code = 'TAL-IT-LAB1' LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.bsit' LIMIT 1),
    'THURSDAY', '09:00:00', '10:00:00', 'LECTURE'
),
(
    (SELECT id FROM class_sections WHERE section_code = 'BSIT-3A' LIMIT 1),
    (SELECT id FROM rooms WHERE code = 'TAL-IT-LAB1' LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.bsit' LIMIT 1),
    'FRIDAY', '13:00:00', '16:00:00', 'LECTURE'
),
(
    (SELECT id FROM class_sections WHERE section_code = 'BSCS-1A' LIMIT 1),
    (SELECT id FROM rooms WHERE code = 'TAL-IT-LAB2' LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.bscs' LIMIT 1),
    'MONDAY', '10:00:00', '12:00:00', 'LABORATORY'
),
(
    (SELECT id FROM class_sections WHERE section_code = 'BSCS-1A' LIMIT 1),
    (SELECT id FROM rooms WHERE code = 'TAL-IT-LAB2' LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.bscs' LIMIT 1),
    'WEDNESDAY', '10:00:00', '11:00:00', 'LECTURE'
),
(
    (SELECT id FROM class_sections WHERE section_code = 'BSCE-1A' LIMIT 1),
    (SELECT id FROM rooms WHERE code = 'TAL-ENG-301' LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.coe' LIMIT 1),
    'TUESDAY', '13:00:00', '15:00:00', 'LECTURE'
),
(
    (SELECT id FROM class_sections WHERE section_code = 'BSCE-1A' LIMIT 1),
    (SELECT id FROM rooms WHERE code = 'TAL-ENG-301' LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.coe' LIMIT 1),
    'THURSDAY', '13:00:00', '15:00:00', 'LECTURE'
),
(
    (SELECT id FROM class_sections WHERE section_code = 'BSME-1A' LIMIT 1),
    (SELECT id FROM rooms WHERE code = 'TAL-ENG-302' LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.bsme' LIMIT 1),
    'WEDNESDAY', '13:00:00', '15:00:00', 'LECTURE'
),
(
    (SELECT id FROM class_sections WHERE section_code = 'BSME-1A' LIMIT 1),
    (SELECT id FROM rooms WHERE code = 'TAL-ENG-302' LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.bsme' LIMIT 1),
    'FRIDAY', '13:00:00', '15:00:00', 'LECTURE'
);

-- -----------------------------------------------------------------------------
-- 14. Student Enrollments & Course Items
-- -----------------------------------------------------------------------------
-- 1. student.bsit.cleared in current term
INSERT INTO student_enrollments (student_id, term_id, enrollment_date, status, total_credit_units, is_overload_approved)
VALUES
(
    (SELECT id FROM student_profiles WHERE student_number = '2026-IT-1001' LIMIT 1),
    (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1),
    NOW(), 'ENROLLED', 3.00, FALSE
)
ON DUPLICATE KEY UPDATE status = 'ENROLLED', total_credit_units = 3.00;

INSERT INTO enrollment_course_items (enrollment_id, section_id, final_numerical_grade, completion_status)
VALUES
(
    (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-IT-1001' LIMIT 1) AND term_id = (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1) LIMIT 1),
    (SELECT id FROM class_sections WHERE section_code = 'BSIT-1A' LIMIT 1),
    NULL, 'ENROLLED'
)
ON DUPLICATE KEY UPDATE completion_status = 'ENROLLED';

-- 2. student.bsit.cleared in historical term (for historical transcript validation)
INSERT INTO student_enrollments (student_id, term_id, enrollment_date, status, total_credit_units, is_overload_approved)
VALUES
(
    (SELECT id FROM student_profiles WHERE student_number = '2026-IT-1001' LIMIT 1),
    (SELECT id FROM terms WHERE term_name = '2nd Semester AY 2025-2026' LIMIT 1),
    '2026-01-15 09:00:00', 'ENROLLED', 3.00, FALSE
)
ON DUPLICATE KEY UPDATE status = 'ENROLLED', total_credit_units = 3.00;

INSERT INTO enrollment_course_items (enrollment_id, section_id, final_numerical_grade, completion_status)
VALUES
(
    (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-IT-1001' LIMIT 1) AND term_id = (SELECT id FROM terms WHERE term_name = '2nd Semester AY 2025-2026' LIMIT 1) LIMIT 1),
    (SELECT id FROM class_sections WHERE section_code = 'BSIT-3A' LIMIT 1),
    1.25, 'PASSED'
)
ON DUPLICATE KEY UPDATE final_numerical_grade = 1.25, completion_status = 'PASSED';

-- 3. student.bsit.blocked in current term
INSERT INTO student_enrollments (student_id, term_id, enrollment_date, status, total_credit_units, is_overload_approved)
VALUES
(
    (SELECT id FROM student_profiles WHERE student_number = '2026-IT-1002' LIMIT 1),
    (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1),
    NOW(), 'ENROLLED', 3.00, FALSE
)
ON DUPLICATE KEY UPDATE status = 'ENROLLED', total_credit_units = 3.00;

INSERT INTO enrollment_course_items (enrollment_id, section_id, final_numerical_grade, completion_status)
VALUES
(
    (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-IT-1002' LIMIT 1) AND term_id = (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1) LIMIT 1),
    (SELECT id FROM class_sections WHERE section_code = 'BSIT-2A' LIMIT 1),
    NULL, 'ENROLLED'
)
ON DUPLICATE KEY UPDATE completion_status = 'ENROLLED';

-- 4. student.bscs.demo in current term
INSERT INTO student_enrollments (student_id, term_id, enrollment_date, status, total_credit_units, is_overload_approved)
VALUES
(
    (SELECT id FROM student_profiles WHERE student_number = '2026-CS-1001' LIMIT 1),
    (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1),
    NOW(), 'ENROLLED', 3.00, FALSE
)
ON DUPLICATE KEY UPDATE status = 'ENROLLED', total_credit_units = 3.00;

INSERT INTO enrollment_course_items (enrollment_id, section_id, final_numerical_grade, completion_status)
VALUES
(
    (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-CS-1001' LIMIT 1) AND term_id = (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1) LIMIT 1),
    (SELECT id FROM class_sections WHERE section_code = 'BSCS-1A' LIMIT 1),
    NULL, 'ENROLLED'
)
ON DUPLICATE KEY UPDATE completion_status = 'ENROLLED';

-- 5. student.bsce.demo in current term
INSERT INTO student_enrollments (student_id, term_id, enrollment_date, status, total_credit_units, is_overload_approved)
VALUES
(
    (SELECT id FROM student_profiles WHERE student_number = '2026-CE-1001' LIMIT 1),
    (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1),
    NOW(), 'ENROLLED', 4.00, FALSE
)
ON DUPLICATE KEY UPDATE status = 'ENROLLED', total_credit_units = 4.00;

INSERT INTO enrollment_course_items (enrollment_id, section_id, final_numerical_grade, completion_status)
VALUES
(
    (SELECT id FROM student_enrollments WHERE student_id = (SELECT id FROM student_profiles WHERE student_number = '2026-CE-1001' LIMIT 1) AND term_id = (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1) LIMIT 1),
    (SELECT id FROM class_sections WHERE section_code = 'BSCE-1A' LIMIT 1),
    NULL, 'ENROLLED'
)
ON DUPLICATE KEY UPDATE completion_status = 'ENROLLED';

-- -----------------------------------------------------------------------------
-- 15. Faculty Workloads
-- -----------------------------------------------------------------------------
INSERT INTO faculty_workloads (term_id, faculty_user_id, regular_units, overload_units, total_contact_hours, is_overload_approved, number_of_preparations)
VALUES
(
    (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.bsit' LIMIT 1),
    6.00, 0.00, 10.00, FALSE, 2
),
(
    (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.bscs' LIMIT 1),
    3.00, 0.00, 5.00, FALSE, 1
),
(
    (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.coe' LIMIT 1),
    4.00, 0.00, 4.00, FALSE, 1
),
(
    (SELECT id FROM terms WHERE is_active = TRUE LIMIT 1),
    (SELECT id FROM users WHERE username = 'faculty.bsme' LIMIT 1),
    4.00, 0.00, 4.00, FALSE, 1
)
ON DUPLICATE KEY UPDATE
    regular_units = VALUES(regular_units),
    total_contact_hours = VALUES(total_contact_hours),
    number_of_preparations = VALUES(number_of_preparations);

-- -----------------------------------------------------------------------------
-- 16. Dynamic Class Record Configurations, Assessment Weightings, and Score Matrices
-- -----------------------------------------------------------------------------
DELETE sgc FROM section_grading_configs sgc
JOIN class_sections s ON sgc.section_id = s.id
WHERE s.section_code IN ('BSIT-1A', 'BSIT-2A', 'BSIT-3A', 'BSCS-1A', 'BSCE-1A', 'BSME-1A');

-- Grading Configs
INSERT INTO section_grading_configs (section_id, midterm_weight, final_weight, is_locked)
VALUES
((SELECT id FROM class_sections WHERE section_code = 'BSIT-1A' LIMIT 1), 50.00, 50.00, FALSE),
((SELECT id FROM class_sections WHERE section_code = 'BSIT-2A' LIMIT 1), 50.00, 50.00, FALSE),
((SELECT id FROM class_sections WHERE section_code = 'BSIT-3A' LIMIT 1), 50.00, 50.00, TRUE),
((SELECT id FROM class_sections WHERE section_code = 'BSCS-1A' LIMIT 1), 50.00, 50.00, FALSE),
((SELECT id FROM class_sections WHERE section_code = 'BSCE-1A' LIMIT 1), 50.00, 50.00, FALSE),
((SELECT id FROM class_sections WHERE section_code = 'BSME-1A' LIMIT 1), 50.00, 50.00, FALSE);

-- Categories for BSIT-1A
INSERT INTO section_grading_categories (config_id, category_name, weight_percentage, term_period, display_order)
VALUES
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-1A' LIMIT 1)), 'Quizzes', 20.00, 'MIDTERM', 1),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-1A' LIMIT 1)), 'Laboratory Activities', 30.00, 'MIDTERM', 2),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-1A' LIMIT 1)), 'Midterm Examination', 50.00, 'MIDTERM', 3),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-1A' LIMIT 1)), 'Quizzes', 20.00, 'FINAL', 1),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-1A' LIMIT 1)), 'Final Project', 30.00, 'FINAL', 2),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-1A' LIMIT 1)), 'Final Examination', 50.00, 'FINAL', 3);

-- Items for BSIT-1A
INSERT INTO class_record_items (category_id, item_title, max_points, sequence_order)
VALUES
(
    (SELECT c.id FROM section_grading_categories c
     JOIN section_grading_configs cfg ON c.config_id = cfg.id
     JOIN class_sections s ON cfg.section_id = s.id
     WHERE s.section_code = 'BSIT-1A' AND c.term_period = 'MIDTERM' AND c.category_name = 'Quizzes' LIMIT 1),
    'Quiz 1: Computer Architecture & Data Representation', 50.00, 1
),
(
    (SELECT c.id FROM section_grading_categories c
     JOIN section_grading_configs cfg ON c.config_id = cfg.id
     JOIN class_sections s ON cfg.section_id = s.id
     WHERE s.section_code = 'BSIT-1A' AND c.term_period = 'MIDTERM' AND c.category_name = 'Quizzes' LIMIT 1),
    'Quiz 2: Operating Systems & Process Management', 50.00, 2
),
(
    (SELECT c.id FROM section_grading_categories c
     JOIN section_grading_configs cfg ON c.config_id = cfg.id
     JOIN class_sections s ON cfg.section_id = s.id
     WHERE s.section_code = 'BSIT-1A' AND c.term_period = 'MIDTERM' AND c.category_name = 'Laboratory Activities' LIMIT 1),
    'Lab Exercise 1: Linux CLI Setup and Scripting', 100.00, 1
),
(
    (SELECT c.id FROM section_grading_categories c
     JOIN section_grading_configs cfg ON c.config_id = cfg.id
     JOIN class_sections s ON cfg.section_id = s.id
     WHERE s.section_code = 'BSIT-1A' AND c.term_period = 'MIDTERM' AND c.category_name = 'Midterm Examination' LIMIT 1),
    'Midterm Departmental Examination', 100.00, 1
);

-- Scores for student.bsit.cleared in BSIT-1A
INSERT INTO student_assessment_scores (item_id, student_id, score_earned, is_excused)
VALUES
(
    (SELECT id FROM class_record_items WHERE item_title = 'Quiz 1: Computer Architecture & Data Representation' LIMIT 1),
    (SELECT id FROM student_profiles WHERE student_number = '2026-IT-1001' LIMIT 1),
    48.00, FALSE
),
(
    (SELECT id FROM class_record_items WHERE item_title = 'Quiz 2: Operating Systems & Process Management' LIMIT 1),
    (SELECT id FROM student_profiles WHERE student_number = '2026-IT-1001' LIMIT 1),
    45.00, FALSE
),
(
    (SELECT id FROM class_record_items WHERE item_title = 'Lab Exercise 1: Linux CLI Setup and Scripting' LIMIT 1),
    (SELECT id FROM student_profiles WHERE student_number = '2026-IT-1001' LIMIT 1),
    95.00, FALSE
),
(
    (SELECT id FROM class_record_items WHERE item_title = 'Midterm Departmental Examination' LIMIT 1),
    (SELECT id FROM student_profiles WHERE student_number = '2026-IT-1001' LIMIT 1),
    92.00, FALSE
);

-- Categories & Items for BSCS-1A
INSERT INTO section_grading_categories (config_id, category_name, weight_percentage, term_period, display_order)
VALUES
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-1A' LIMIT 1)), 'Quizzes', 20.00, 'MIDTERM', 1),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-1A' LIMIT 1)), 'Laboratory Activities', 30.00, 'MIDTERM', 2),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-1A' LIMIT 1)), 'Midterm Examination', 50.00, 'MIDTERM', 3),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-1A' LIMIT 1)), 'Quizzes', 20.00, 'FINAL', 1),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-1A' LIMIT 1)), 'Final Project', 30.00, 'FINAL', 2),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCS-1A' LIMIT 1)), 'Final Examination', 50.00, 'FINAL', 3);

INSERT INTO class_record_items (category_id, item_title, max_points, sequence_order)
VALUES
(
    (SELECT c.id FROM section_grading_categories c
     JOIN section_grading_configs cfg ON c.config_id = cfg.id
     JOIN class_sections s ON cfg.section_id = s.id
     WHERE s.section_code = 'BSCS-1A' AND c.term_period = 'MIDTERM' AND c.category_name = 'Quizzes' LIMIT 1),
    'Quiz 1: Logic and Boolean Functions', 50.00, 1
);

INSERT INTO student_assessment_scores (item_id, student_id, score_earned, is_excused)
VALUES
(
    (SELECT id FROM class_record_items WHERE item_title = 'Quiz 1: Logic and Boolean Functions' LIMIT 1),
    (SELECT id FROM student_profiles WHERE student_number = '2026-CS-1001' LIMIT 1),
    47.00, FALSE
);

-- Categories & Items for BSCE-1A
INSERT INTO section_grading_categories (config_id, category_name, weight_percentage, term_period, display_order)
VALUES
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCE-1A' LIMIT 1)), 'Problem Sets', 25.00, 'MIDTERM', 1),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCE-1A' LIMIT 1)), 'Quizzes', 25.00, 'MIDTERM', 2),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCE-1A' LIMIT 1)), 'Midterm Examination', 50.00, 'MIDTERM', 3),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCE-1A' LIMIT 1)), 'Problem Sets', 25.00, 'FINAL', 1),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCE-1A' LIMIT 1)), 'Quizzes', 25.00, 'FINAL', 2),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSCE-1A' LIMIT 1)), 'Final Examination', 50.00, 'FINAL', 3);

INSERT INTO class_record_items (category_id, item_title, max_points, sequence_order)
VALUES
(
    (SELECT c.id FROM section_grading_categories c
     JOIN section_grading_configs cfg ON c.config_id = cfg.id
     JOIN class_sections s ON cfg.section_id = s.id
     WHERE s.section_code = 'BSCE-1A' AND c.term_period = 'MIDTERM' AND c.category_name = 'Problem Sets' LIMIT 1),
    'Problem Set 1: Derivatives and Rate of Change', 50.00, 1
);

INSERT INTO student_assessment_scores (item_id, student_id, score_earned, is_excused)
VALUES
(
    (SELECT id FROM class_record_items WHERE item_title = 'Problem Set 1: Derivatives and Rate of Change' LIMIT 1),
    (SELECT id FROM student_profiles WHERE student_number = '2026-CE-1001' LIMIT 1),
    46.00, FALSE
);

-- Categories for BSIT-2A, BSIT-3A, BSME-1A
INSERT INTO section_grading_categories (config_id, category_name, weight_percentage, term_period, display_order)
VALUES
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-2A' LIMIT 1)), 'Quizzes', 50.00, 'MIDTERM', 1),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-2A' LIMIT 1)), 'Midterm Examination', 50.00, 'MIDTERM', 2),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-2A' LIMIT 1)), 'Quizzes', 50.00, 'FINAL', 1),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-2A' LIMIT 1)), 'Final Examination', 50.00, 'FINAL', 2),

((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-3A' LIMIT 1)), 'Quizzes', 50.00, 'MIDTERM', 1),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-3A' LIMIT 1)), 'Midterm Examination', 50.00, 'MIDTERM', 2),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-3A' LIMIT 1)), 'Quizzes', 50.00, 'FINAL', 1),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSIT-3A' LIMIT 1)), 'Final Examination', 50.00, 'FINAL', 2),

((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSME-1A' LIMIT 1)), 'Quizzes', 50.00, 'MIDTERM', 1),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSME-1A' LIMIT 1)), 'Midterm Examination', 50.00, 'MIDTERM', 2),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSME-1A' LIMIT 1)), 'Quizzes', 50.00, 'FINAL', 1),
((SELECT id FROM section_grading_configs WHERE section_id = (SELECT id FROM class_sections WHERE section_code = 'BSME-1A' LIMIT 1)), 'Final Examination', 50.00, 'FINAL', 2);

SET FOREIGN_KEY_CHECKS = 1;
