-- V4__seed_curriculum_catalog_and_obe.sql
-- Subsystem: Academic Curriculum, Course Catalogs, and OBE Framework
-- Scope: Master Course Registry, Program Curricula, Term Block Offerings, Prerequisites, and OBE Competency Matrix

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. Master Course Catalog (General Education & Professional Majors)
-- -----------------------------------------------------------------------------
INSERT INTO courses (id, code, title, lecture_units, lab_units, credit_units, contact_hours_lec, contact_hours_lab, category, description, is_active)
VALUES
-- General Education & Mandated Courses
(1, 'GE-USELF', 'Understanding the Self', 3.00, 0.00, 3.00, 3, 0, 'GEN_ED', 'General Education | CMO 20 s.2013 | Target: All Programs', TRUE),
(2, 'GE-RPH', 'Readings in Philippine History', 3.00, 0.00, 3.00, 3, 0, 'GEN_ED', 'General Education | CMO 20 s.2013 | Target: All Programs', TRUE),
(3, 'GE-PC', 'Purposive Communication', 3.00, 0.00, 3.00, 3, 0, 'GEN_ED', 'General Education | CMO 20 s.2013 | Target: All Programs', TRUE),
(4, 'GE-MMW', 'Mathematics in the Modern World', 3.00, 0.00, 3.00, 3, 0, 'GEN_ED', 'General Education | CMO 20 s.2013 | Target: All Programs', TRUE),
(5, 'PE-1', 'Physical Activities Towards Health and Fitness 1', 2.00, 0.00, 2.00, 2, 0, 'MANDATED', 'Physical Education | CMO 80 s.2017 | Target: All Programs', TRUE),
(6, 'NSTP-1', 'National Service Training Program 1', 3.00, 0.00, 3.00, 3, 0, 'MANDATED', 'National Service Training Program | RA 9163 | Target: All Programs', TRUE),
(7, 'GE-CW', 'The Contemporary World', 3.00, 0.00, 3.00, 3, 0, 'GEN_ED', 'General Education | CMO 20 s.2013 | Target: All Programs', TRUE),
(8, 'GE-ART', 'Art Appreciation', 3.00, 0.00, 3.00, 3, 0, 'GEN_ED', 'General Education | CMO 20 s.2013 | Target: All Programs', TRUE),
(9, 'PE-2', 'Physical Activities Towards Health and Fitness 2', 2.00, 0.00, 2.00, 2, 0, 'MANDATED', 'Physical Education | CMO 80 s.2017 | Target: All Programs', TRUE),
(10, 'NSTP-2', 'National Service Training Program 2', 3.00, 0.00, 3.00, 3, 0, 'MANDATED', 'National Service Training Program | RA 9163 | Target: All Programs', TRUE),
(11, 'GE-STS', 'Science, Technology, and Society', 3.00, 0.00, 3.00, 3, 0, 'GEN_ED', 'General Education | CMO 20 s.2013 | Target: All Programs', TRUE),
(12, 'GE-ETH', 'Ethics', 3.00, 0.00, 3.00, 3, 0, 'GEN_ED', 'General Education | CMO 20 s.2013 | Target: All Programs', TRUE),
(13, 'GE-RIZAL', 'Life and Works of Rizal', 3.00, 0.00, 3.00, 3, 0, 'MANDATED', 'Mandated Subject | RA 1425 | Target: All Programs', TRUE),

-- Computing & IT Major Courses
(14, 'IT-101', 'Introduction to Computing', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'CC 101 | CMO 25 s.2015 | IT & CS Core', TRUE),
(15, 'IT-102', 'Fundamentals of Programming', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'CC 102 | CMO 25 s.2015 | IT & CS Core', TRUE),
(16, 'IT-201', 'Data Structures and Algorithms', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'CC 103 | CMO 25 s.2015 | IT & CS Core', TRUE),
(17, 'IT-202', 'Object-Oriented Programming', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'CC 104 | CMO 25 s.2015 | IT & CS Core', TRUE),
(18, 'IT-203', 'Information Management', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'IM 101 | CMO 25 s.2015 | Relational Database Systems', TRUE),
(19, 'IT-204', 'Web Systems and Technologies', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'WS 101 | CMO 25 s.2015 | Full-stack Web Architecture', TRUE),
(20, 'IT-NET1', 'Networking 1', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'NET 101 | CMO 25 s.2015 | Networking Fundamentals', TRUE),
(21, 'IT-SEC1', 'Information Assurance and Security 1', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'IAS 101 | CMO 25 s.2015 | Cybersecurity Foundations', TRUE),
(22, 'IT-CAP1', 'Capstone Project and Research 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'CAP 101 | Proposal Stage | Target: BSIT', TRUE),
(23, 'IT-CAP2', 'Capstone Project and Research 2', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'CAP 102 | Defense & Implementation | Target: BSIT', TRUE),
(24, 'CS-101', 'Discrete Structures', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Discrete Mathematics for Computing | CMO 25 s.2015', TRUE),
(25, 'CS-102', 'Algorithms and Complexity', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Advanced Complexity Theory | CMO 25 s.2015', TRUE),

-- Multidisciplinary Core Courses
(26, 'EL-101', 'Introduction to Language Studies', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Language Fundamentals | CMO 24 s.2017', TRUE),
(27, 'EL-102', 'Language of Literary Texts', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Literary Stylistics | CMO 24 s.2017', TRUE),
(28, 'EL-201', 'Structure of English', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Morphology & Syntax | CMO 24 s.2017', TRUE),
(29, 'PA-101', 'Introduction to Public Administration', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Foundations of Public Governance | CMO 06 s.2018', TRUE),
(30, 'PA-201', 'Philippine Administrative System', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Bureaucratic Structures | CMO 06 s.2018', TRUE),
(31, 'PA-301', 'Public Fiscal Administration', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Public Budgeting & Revenue | CMO 06 s.2018', TRUE),
(32, 'PSY-101', 'Introduction to Psychology', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Psychological Principles | CMO 34 s.2017', TRUE),
(33, 'MATH-101', 'Calculus 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Differential Calculus | CMO 19 s.2007', TRUE),
(34, 'MATH-102', 'Calculus 2', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Integral Calculus | CMO 19 s.2007', TRUE),
(35, 'ACT-101', 'Financial Accounting & Reporting', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Accounting Cycle | CMO 27 s.2017', TRUE),
(36, 'ACT-102', 'Conceptual Framework & Accounting Standards', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'PFRS & IFRS Framework | CMO 27 s.2017', TRUE),
(37, 'CE-101', 'Civil Engineering Orientation', 1.00, 1.00, 2.00, 1, 3, 'PROFESSIONAL_MAJOR', 'CE Field Practice | CMO 92 s.2017', TRUE),
(38, 'CE-102', 'Statics of Rigid Bodies', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Engineering Mechanics | CMO 92 s.2017', TRUE),
(39, 'CPE-101', 'Computer Engineering as a Discipline', 1.00, 0.00, 1.00, 1, 0, 'PROFESSIONAL_MAJOR', 'CpE Introduction | CMO 87 s.2017', TRUE),
(40, 'CRIM-101', 'Introduction to Philippine Criminal Justice System', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'PCJS Overview | CMO 05 s.2018', TRUE)
ON DUPLICATE KEY UPDATE title = VALUES(title),
                        lecture_units = VALUES(lecture_units),
                        lab_units = VALUES(lab_units),
                        credit_units = VALUES(credit_units),
                        contact_hours_lec = VALUES(contact_hours_lec),
                        contact_hours_lab = VALUES(contact_hours_lab),
                        category = VALUES(category),
                        description = VALUES(description),
                        is_active = VALUES(is_active);

-- -----------------------------------------------------------------------------
-- 2. Program Curricula
-- -----------------------------------------------------------------------------
INSERT INTO curricula (id, program_id, code, name, status, version_number, effective_academic_year, is_active)
VALUES
(1, (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'BSIT-2026', 'Bachelor of Science in Information Technology Curriculum 2026-2030', 'ACTIVE', 1, '2026-2027', TRUE),
(2, (SELECT id FROM programs WHERE code = 'BSIS' LIMIT 1), 'BSIS-2026', 'Bachelor of Science in Information Systems Curriculum 2026-2030', 'ACTIVE', 1, '2026-2027', TRUE),
(3, (SELECT id FROM programs WHERE code = 'BSCS' LIMIT 1), 'BSCS-2026', 'Bachelor of Science in Computer Science Curriculum 2026-2030', 'ACTIVE', 1, '2026-2027', TRUE),
(4, (SELECT id FROM programs WHERE code = 'BSCE' LIMIT 1), 'BSCE-2026', 'Bachelor of Science in Civil Engineering Curriculum 2026-2030', 'ACTIVE', 1, '2026-2027', TRUE),
(5, (SELECT id FROM programs WHERE code = 'BSCpE' LIMIT 1), 'BSCpE-2026', 'Bachelor of Science in Computer Engineering Curriculum 2026-2030', 'ACTIVE', 1, '2026-2027', TRUE),
(6, (SELECT id FROM programs WHERE code = 'BAEL' LIMIT 1), 'BAEL-2026', 'Bachelor of Arts in English Language Curriculum 2026-2030', 'ACTIVE', 1, '2026-2027', TRUE),
(7, (SELECT id FROM programs WHERE code = 'BASS' LIMIT 1), 'BASS-2026', 'Bachelor of Arts in Social Science Curriculum 2026-2030', 'ACTIVE', 1, '2026-2027', TRUE),
(8, (SELECT id FROM programs WHERE code = 'BPA' LIMIT 1), 'BPA-2026', 'Bachelor of Public Administration Curriculum 2026-2030', 'ACTIVE', 1, '2026-2027', TRUE),
(9, (SELECT id FROM programs WHERE code = 'BSA' LIMIT 1), 'BSA-2026', 'Bachelor of Science in Accountancy Curriculum 2026-2030', 'ACTIVE', 1, '2026-2027', TRUE),
(10, (SELECT id FROM programs WHERE code = 'BSCrim' LIMIT 1), 'BSCrim-2026', 'Bachelor of Science in Criminology Curriculum 2026-2030', 'ACTIVE', 1, '2026-2027', TRUE)
ON DUPLICATE KEY UPDATE program_id = VALUES(program_id),
                        name = VALUES(name),
                        status = VALUES(status),
                        version_number = VALUES(version_number),
                        effective_academic_year = VALUES(effective_academic_year),
                        is_active = VALUES(is_active);

-- -----------------------------------------------------------------------------
-- 3. Curriculum Course Offerings (Term-Level Blocks)
-- -----------------------------------------------------------------------------
INSERT INTO curriculum_courses (curriculum_id, course_id, year_level, sequence_order, category, semester)
VALUES
-- BSIT 1st Year - 1st Semester
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-USELF' LIMIT 1), 1, 1, 'GEN_ED', '1ST_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-RPH' LIMIT 1), 1, 2, 'GEN_ED', '1ST_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-PC' LIMIT 1), 1, 3, 'GEN_ED', '1ST_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-MMW' LIMIT 1), 1, 4, 'GEN_ED', '1ST_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'PE-1' LIMIT 1), 1, 5, 'MANDATED', '1ST_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'NSTP-1' LIMIT 1), 1, 6, 'MANDATED', '1ST_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-101' LIMIT 1), 1, 7, 'PROFESSIONAL_MAJOR', '1ST_SEM'),

-- BSIT 1st Year - 2nd Semester
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-CW' LIMIT 1), 1, 1, 'GEN_ED', '2ND_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-ART' LIMIT 1), 1, 2, 'GEN_ED', '2ND_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'PE-2' LIMIT 1), 1, 3, 'MANDATED', '2ND_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'NSTP-2' LIMIT 1), 1, 4, 'MANDATED', '2ND_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-102' LIMIT 1), 1, 5, 'PROFESSIONAL_MAJOR', '2ND_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS-101' LIMIT 1), 1, 6, 'PROFESSIONAL_MAJOR', '2ND_SEM'),

-- BSIT 2nd Year - 1st Semester
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-STS' LIMIT 1), 2, 1, 'GEN_ED', '1ST_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-201' LIMIT 1), 2, 2, 'PROFESSIONAL_MAJOR', '1ST_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-202' LIMIT 1), 2, 3, 'PROFESSIONAL_MAJOR', '1ST_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-NET1' LIMIT 1), 2, 4, 'PROFESSIONAL_MAJOR', '1ST_SEM'),

-- BSIT 2nd Year - 2nd Semester
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-ETH' LIMIT 1), 2, 1, 'GEN_ED', '2ND_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-RIZAL' LIMIT 1), 2, 2, 'MANDATED', '2ND_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-203' LIMIT 1), 2, 3, 'PROFESSIONAL_MAJOR', '2ND_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-204' LIMIT 1), 2, 4, 'PROFESSIONAL_MAJOR', '2ND_SEM'),
((SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-SEC1' LIMIT 1), 2, 5, 'PROFESSIONAL_MAJOR', '2ND_SEM')
ON DUPLICATE KEY UPDATE year_level = VALUES(year_level),
                        sequence_order = VALUES(sequence_order),
                        category = VALUES(category),
                        semester = VALUES(semester);

-- -----------------------------------------------------------------------------
-- 4. Course Prerequisite DAG Rules
-- -----------------------------------------------------------------------------
INSERT INTO course_prerequisites (course_id, prerequisite_course_id, rule_type, min_grade_required)
VALUES
((SELECT id FROM courses WHERE code = 'IT-102' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-101' LIMIT 1), 'HARD', '3.00'),
((SELECT id FROM courses WHERE code = 'IT-201' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-102' LIMIT 1), 'HARD', '3.00'),
((SELECT id FROM courses WHERE code = 'IT-202' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-102' LIMIT 1), 'HARD', '3.00'),
((SELECT id FROM courses WHERE code = 'IT-203' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-201' LIMIT 1), 'HARD', '3.00'),
((SELECT id FROM courses WHERE code = 'IT-204' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-102' LIMIT 1), 'HARD', '3.00'),
((SELECT id FROM courses WHERE code = 'IT-CAP2' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-CAP1' LIMIT 1), 'HARD', '3.00'),
((SELECT id FROM courses WHERE code = 'PE-2' LIMIT 1), (SELECT id FROM courses WHERE code = 'PE-1' LIMIT 1), 'HARD', '3.00'),
((SELECT id FROM courses WHERE code = 'NSTP-2' LIMIT 1), (SELECT id FROM courses WHERE code = 'NSTP-1' LIMIT 1), 'HARD', '3.00'),
((SELECT id FROM courses WHERE code = 'CS-102' LIMIT 1), (SELECT id FROM courses WHERE code = 'CS-101' LIMIT 1), 'HARD', '3.00'),
((SELECT id FROM courses WHERE code = 'MATH-102' LIMIT 1), (SELECT id FROM courses WHERE code = 'MATH-101' LIMIT 1), 'HARD', '3.00'),
((SELECT id FROM courses WHERE code = 'ACT-102' LIMIT 1), (SELECT id FROM courses WHERE code = 'ACT-101' LIMIT 1), 'HARD', '3.00')
ON DUPLICATE KEY UPDATE rule_type = VALUES(rule_type),
                        min_grade_required = VALUES(min_grade_required);

-- -----------------------------------------------------------------------------
-- 5. Outcome-Based Education (OBE): PILOs
-- -----------------------------------------------------------------------------
INSERT INTO program_outcomes (program_id, code, description)
VALUES
((SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'PILO-GE-01', 'Critical Thinking & Quantitative Logic: Apply analytical, critical, and creative thinking, as well as quantitative logic across disciplines.'),
((SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'PILO-GE-02', 'Effective & Ethical Communication: Convey complex concepts clearly, ethically, and persuasively in multimodal formats.'),
((SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'PILO-IT-01', 'System Analysis & Design: Analyze complex computing problems and apply principles of computing to identify solutions.'),
((SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'PILO-IT-02', 'Software Implementation: Design, implement, and evaluate a computing-based solution meeting a given set of computing requirements.'),
((SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'PILO-IT-03', 'Information Assurance: Apply cybersecurity principles and best practices to maintain operations during security events.')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- -----------------------------------------------------------------------------
-- 6. Course Intended Learning Outcomes (CILOs)
-- -----------------------------------------------------------------------------
INSERT INTO course_outcomes (course_id, code, description, blooms_level)
VALUES
((SELECT id FROM courses WHERE code = 'IT-101' LIMIT 1), 'CILO-IT101-1', 'Explain the foundational hardware, software, and networking architectures of modern computing systems.', 'UNDERSTAND'),
((SELECT id FROM courses WHERE code = 'IT-101' LIMIT 1), 'CILO-IT101-2', 'Evaluate computing solutions adhering to professional ethics and legal standards.', 'EVALUATE'),
((SELECT id FROM courses WHERE code = 'IT-102' LIMIT 1), 'CILO-IT102-1', 'Design algorithmic solutions to computational problems using structured programming constructs.', 'APPLY'),
((SELECT id FROM courses WHERE code = 'IT-102' LIMIT 1), 'CILO-IT102-2', 'Write, compile, and debug maintainable code utilizing standard control structures and functions.', 'CREATE'),
((SELECT id FROM courses WHERE code = 'IT-201' LIMIT 1), 'CILO-IT201-1', 'Analyze runtime and space complexity of fundamental data structures and sorting algorithms.', 'ANALYZE'),
((SELECT id FROM courses WHERE code = 'IT-201' LIMIT 1), 'CILO-IT201-2', 'Implement non-linear data structures such as trees and graphs for efficient data access.', 'CREATE')
ON DUPLICATE KEY UPDATE description = VALUES(description),
                        blooms_level = VALUES(blooms_level);

-- -----------------------------------------------------------------------------
-- 7. CILO to PILO Alignment Matrix Mappings
-- -----------------------------------------------------------------------------
INSERT INTO cilo_pilo_mappings (course_outcome_id, program_outcome_id, mapping_type)
VALUES
((SELECT id FROM course_outcomes WHERE code = 'CILO-IT101-1' LIMIT 1), (SELECT id FROM program_outcomes WHERE code = 'PILO-IT-01' LIMIT 1), 'I'),
((SELECT id FROM course_outcomes WHERE code = 'CILO-IT101-2' LIMIT 1), (SELECT id FROM program_outcomes WHERE code = 'PILO-GE-02' LIMIT 1), 'I'),
((SELECT id FROM course_outcomes WHERE code = 'CILO-IT102-1' LIMIT 1), (SELECT id FROM program_outcomes WHERE code = 'PILO-IT-01' LIMIT 1), 'E'),
((SELECT id FROM course_outcomes WHERE code = 'CILO-IT102-2' LIMIT 1), (SELECT id FROM program_outcomes WHERE code = 'PILO-IT-02' LIMIT 1), 'E'),
((SELECT id FROM course_outcomes WHERE code = 'CILO-IT201-1' LIMIT 1), (SELECT id FROM program_outcomes WHERE code = 'PILO-GE-01' LIMIT 1), 'D'),
((SELECT id FROM course_outcomes WHERE code = 'CILO-IT201-2' LIMIT 1), (SELECT id FROM program_outcomes WHERE code = 'PILO-IT-02' LIMIT 1), 'D')
ON DUPLICATE KEY UPDATE mapping_type = VALUES(mapping_type);

SET FOREIGN_KEY_CHECKS = 1;
