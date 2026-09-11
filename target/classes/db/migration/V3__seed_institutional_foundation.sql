-- V3__seed_institutional_foundation.sql
-- Subsystem: Academic Infrastructure & Institutional Foundation
-- Scope: Campuses, Colleges, Academic Departments, Academic Years, Terms, and Degree Programs

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. Campuses (CHMSU Multi-Campus System)
-- -----------------------------------------------------------------------------
INSERT INTO campuses (id, code, ched_institutional_code, name, address, region, contact_number, email, is_main, is_active)
VALUES (1, 'TALISAY', '06014', 'CHMSU - Talisay (Main Campus)', 'Mabini St., Brgy. Zone 1, Talisay City, Negros Occidental', 'REGION VI', '(034) 712-0005', 'info@chmsu.edu.ph', TRUE, TRUE),
       (2, 'ALIIJIS', '06015', 'CHMSU - Alijis Campus', 'Brgy. Alijis, Bacolod City, Negros Occidental', 'REGION VI', '(034) 434-4043', 'alijis.director@chmsu.edu.ph', FALSE, TRUE),
       (3, 'BINALBAGAN', '06016', 'CHMSU - Binalbagan Campus', 'Brgy. Enclaro, Binalbagan, Negros Occidental', 'REGION VI', '(034) 388-8264', 'binalbagan.campus@chmsu.edu.ph', FALSE, TRUE),
       (4, 'FORTUNE', '06017', 'CHMSU - Fortune Towne Campus', 'Brgy. Estefania, Bacolod City, Negros Occidental', 'REGION VI', '(034) 433-5210', 'fortunetowne@chmsu.edu.ph', FALSE, TRUE)
ON DUPLICATE KEY UPDATE ched_institutional_code = VALUES(ched_institutional_code),
                        name = VALUES(name),
                        address = VALUES(address),
                        region = VALUES(region),
                        contact_number = VALUES(contact_number),
                        email = VALUES(email),
                        is_main = VALUES(is_main),
                        is_active = VALUES(is_active);

-- -----------------------------------------------------------------------------
-- 2. Academic Colleges & Functional Departments
-- -----------------------------------------------------------------------------
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
(9, 3, 'CBMA_B', 'College of Business Management and Accountancy - Binalbagan', 'COLLEGE', NULL, TRUE),
(10, 4, 'CBMA_FT', 'College of Business Management and Accountancy - Fortune Towne', 'COLLEGE', NULL, TRUE),
(11, 4, 'CPAG', 'College of Public Administration and Governance', 'COLLEGE', NULL, TRUE),
(17, 1, 'COE', 'College of Engineering', 'COLLEGE', NULL, TRUE),
(18, 1, 'CCJ', 'College of Criminal Justice', 'COLLEGE', NULL, TRUE),
(19, 3, 'COF', 'College of Fisheries', 'COLLEGE', NULL, TRUE),
(20, 4, 'CBMA', 'College of Business Management & Accountancy', 'COLLEGE', NULL, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name),
                        type = VALUES(type),
                        parent_department_id = VALUES(parent_department_id),
                        is_active = VALUES(is_active);

INSERT INTO departments (id, campus_id, code, name, type, parent_department_id, is_active)
VALUES (12, 2, 'IT_DEPT', 'Department of Information Technology', 'DEPARTMENT', 7, TRUE),
       (13, 2, 'IS_DEPT', 'Department of Information Systems', 'DEPARTMENT', 7, TRUE),
       (14, 1, 'SED_DEPT', 'Department of Secondary Education', 'DEPARTMENT', 3, TRUE),
       (15, 1, 'MATH_DEPT', 'Department of Mathematics and Natural Sciences', 'DEPARTMENT', 4, TRUE),
       (16, 4, 'ACTG_DEPT', 'Department of Accountancy', 'DEPARTMENT', 10, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name),
                        type = VALUES(type),
                        parent_department_id = VALUES(parent_department_id),
                        is_active = VALUES(is_active);

-- -----------------------------------------------------------------------------
-- 3. Academic Calendar: Academic Years & Terms
-- -----------------------------------------------------------------------------
INSERT INTO academic_years (id, code, start_date, end_date, is_current)
VALUES (1, 'AY 2025-2026', '2025-08-11', '2026-07-17', FALSE),
       (2, 'AY 2026-2027', '2026-08-10', '2027-07-16', TRUE)
ON DUPLICATE KEY UPDATE start_date = VALUES(start_date),
                        end_date = VALUES(end_date),
                        is_current = VALUES(is_current);

INSERT INTO terms (id, academic_year_id, term_type, start_date, end_date, enrollment_open, grading_open, add_drop_open, is_active, max_hours_per_class)
VALUES (1, 2, 'FIRST_SEM', '2026-08-10', '2026-12-18', TRUE, FALSE, TRUE, TRUE, 3.0),
       (2, 2, 'SECOND_SEM', '2027-01-18', '2027-05-28', FALSE, FALSE, FALSE, FALSE, 3.0),
       (3, 2, 'SUMMER', '2027-06-14', '2027-07-23', FALSE, FALSE, FALSE, FALSE, 3.0)
ON DUPLICATE KEY UPDATE start_date = VALUES(start_date),
                        end_date = VALUES(end_date),
                        enrollment_open = VALUES(enrollment_open),
                        grading_open = VALUES(grading_open),
                        add_drop_open = VALUES(add_drop_open),
                        is_active = VALUES(is_active),
                        max_hours_per_class = VALUES(max_hours_per_class);

-- -----------------------------------------------------------------------------
-- 4. Degree Programs & CMO Matrices (with College Scoping)
-- -----------------------------------------------------------------------------
INSERT INTO programs (id, department_id, college_id, code, name, major, degree_level, ched_cmo_reference, government_permit, total_units_required, is_active)
VALUES
(1, 12, 7, 'BSIT', 'Bachelor of Science in Information Technology', 'Network, Security & Web Systems', 'UNDERGRADUATE', 'CMO No. 25, Series 2015', 'PSG for Information Technology', 146, TRUE),
(2, 13, 7, 'BSIS', 'Bachelor of Science in Information Systems', 'Enterprise Architecture & Analytics', 'UNDERGRADUATE', 'CMO No. 25, Series 2015', 'PSG for Information Systems', 146, TRUE),
(3, 12, 7, 'BSCS', 'Bachelor of Science in Computer Science', 'Algorithms & Intelligent Systems', 'UNDERGRADUATE', 'CMO No. 25, Series 2015', 'PSG for Computer Science', 146, TRUE),
(4, 5, 17, 'BSCE', 'Bachelor of Science in Civil Engineering', 'Structural & Water Resources', 'UNDERGRADUATE', 'CMO No. 92, Series 2017', 'PSG for Civil Engineering', 160, TRUE),
(5, 5, 17, 'BSCpE', 'Bachelor of Science in Computer Engineering', 'Embedded Systems & IoT', 'UNDERGRADUATE', 'CMO No. 87, Series 2017', 'PSG for Computer Engineering', 160, TRUE),
(6, 5, 17, 'BSECE', 'Bachelor of Science in Electronics Engineering', 'Telecommunications & Signals', 'UNDERGRADUATE', 'CMO No. 101, Series 2017', 'PSG for Electronics Engineering', 160, TRUE),
(7, 5, 17, 'BSME', 'Bachelor of Science in Mechanical Engineering', 'Mechanical & Thermal Systems', 'UNDERGRADUATE', 'CMO No. 97, Series 2017', 'PSG for Mechanical Engineering', 160, TRUE),
(8, 4, 4, 'BAEL', 'Bachelor of Arts in English Language', 'Language Studies', 'UNDERGRADUATE', 'CMO No. 24, Series 2017', 'PSG for English Language Studies', 136, TRUE),
(9, 4, 4, 'BASS', 'Bachelor of Arts in Social Science', 'General / Social Studies', 'UNDERGRADUATE', 'CMO No. 36, Series 2017', 'PSG for Social Science', 136, TRUE),
(10, 4, 4, 'BPA', 'Bachelor of Public Administration', 'Public Governance', 'UNDERGRADUATE', 'CMO No. 06, Series 2018', 'PSG for Public Administration', 146, TRUE),
(11, 15, 4, 'BSAM', 'Bachelor of Science in Applied Mathematics', 'Applied / Industrial Math', 'UNDERGRADUATE', 'CMO No. 19, Series 2007', 'PSG for Applied Mathematics', 146, TRUE),
(12, 4, 4, 'BS-PSYCH', 'Bachelor of Science in Psychology', 'Clinical / Industrial Track', 'UNDERGRADUATE', 'CMO No. 34, Series 2017', 'PSG for Psychology', 146, TRUE),
(13, 16, 20, 'BSA', 'Bachelor of Science in Accountancy', 'CPA Professional Track', 'UNDERGRADUATE', 'CMO No. 27, Series 2017', 'PSG for Accountancy', 156, TRUE),
(14, 16, 20, 'BSMA', 'Bachelor of Science in Management Accounting', 'Management Accounting Track', 'UNDERGRADUATE', 'CMO No. 28, Series 2017', 'PSG for Management Accounting', 146, TRUE),
(15, 10, 20, 'BSBA', 'Bachelor of Science in Business Administration', 'Financial Management', 'UNDERGRADUATE', 'CMO No. 17, Series 2017', 'PSG for Business Administration', 146, TRUE),
(16, 10, 20, 'BSE', 'Bachelor of Science in Entrepreneurship', 'Innovation & Enterprise Development', 'UNDERGRADUATE', 'CMO No. 18, Series 2017', 'PSG for Entrepreneurship', 146, TRUE),
(17, 10, 20, 'BSHM', 'Bachelor of Science in Hospitality Management', 'Hotel & Foodservice Operations', 'UNDERGRADUATE', 'CMO No. 62, Series 2017', 'PSG for Hospitality Management', 146, TRUE),
(18, 10, 20, 'BSOA', 'Bachelor of Science in Office Administration', 'Corporate Office Administration', 'UNDERGRADUATE', 'CMO No. 19, Series 2017', 'PSG for Office Administration', 146, TRUE),
(19, 14, 3, 'BECEd', 'Bachelor of Early Childhood Education', 'Early Childhood Development', 'UNDERGRADUATE', 'CMO No. 76, Series 2017', 'PSG for Early Childhood Education', 136, TRUE),
(20, 14, 3, 'BEEd', 'Bachelor of Elementary Education', 'General Elementary Pedagogy', 'UNDERGRADUATE', 'CMO No. 74, Series 2017', 'PSG for Elementary Education', 136, TRUE),
(21, 14, 3, 'BSNEd', 'Bachelor of Special Needs Education', 'Early Intervention / General', 'UNDERGRADUATE', 'CMO No. 77, Series 2017', 'PSG for Special Needs Education', 136, TRUE),
(22, 14, 3, 'BPEd', 'Bachelor of Physical Education', 'School PE & Coaching', 'UNDERGRADUATE', 'CMO No. 80, Series 2017', 'PSG for Physical Education', 136, TRUE),
(23, 14, 3, 'BSEd', 'Bachelor of Secondary Education', 'Majors: English, Math, Sci, Fil', 'UNDERGRADUATE', 'CMO No. 75, Series 2017', 'PSG for Secondary Education', 136, TRUE),
(24, 14, 3, 'BTLEd', 'Bachelor of Technology & Livelihood Educ.', 'Home Economics / Industrial Arts', 'UNDERGRADUATE', 'CMO No. 78, Series 2017', 'PSG for Technology & Livelihood Education', 146, TRUE),
(25, 14, 3, 'BTVTEd', 'Bachelor of Technical-Vocational Teacher Educ.', 'Electrical / Electronics Tech', 'UNDERGRADUATE', 'CMO No. 79, Series 2017', 'PSG for Technical-Vocational Teacher Education', 146, TRUE),
(26, 6, 6, 'BIT', 'Bachelor of Industrial Technology', 'Drafting, Automotive, Electrical', 'UNDERGRADUATE', 'CMO No. 20, Series 2014', 'PSG for Industrial Technology', 146, TRUE),
(27, 18, 18, 'BSCrim', 'Bachelor of Science in Criminology', 'Law Enforcement & Forensics', 'UNDERGRADUATE', 'CMO No. 05, Series 2018', 'PSG for Criminology', 146, TRUE),
(28, 8, 8, 'BSFi', 'Bachelor of Science in Fisheries', 'Aquaculture & Post-Harvest', 'UNDERGRADUATE', 'CMO No. 43, Series 2017', 'PSG for Fisheries', 136, TRUE)
ON DUPLICATE KEY UPDATE department_id = VALUES(department_id),
                        college_id = VALUES(college_id),
                        name = VALUES(name),
                        major = VALUES(major),
                        degree_level = VALUES(degree_level),
                        ched_cmo_reference = VALUES(ched_cmo_reference),
                        government_permit = VALUES(government_permit),
                        total_units_required = VALUES(total_units_required),
                        is_active = VALUES(is_active);

SET FOREIGN_KEY_CHECKS = 1;
