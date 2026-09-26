-- V102__seed_chmsu_programs_majors_curricula_and_course_mappings.sql
-- Subsystem: CHMSU Academic Programs, Majors, Master Courses, Curricula and Exhaustive 4-Year Mappings

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE majors CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- ============================================================================
-- 1. CAMPUSES & COLLEGES (DEPARTMENTS)
-- ============================================================================

INSERT INTO campuses (code, ched_institutional_code, name, address, region, contact_number, email, is_main, is_active)
VALUES 
    ('TALISAY', '06001', 'Talisay Main Campus', 'Mabini St., Talisay City, Negros Occidental', 'REGION VI', '(034) 712-0000', 'talisay.main@chmsu.edu.ph', TRUE, TRUE),
    ('ALIJIS', '06004', 'Alijis Campus', 'Brgy. Alijis, Bacolod City', 'REGION VI', '(034) 432-8899', 'alijis@chmsu.edu.ph', FALSE, TRUE),
    ('FORTUNE_TOWNE', '06002', 'Fortune Towne Campus', 'Brgy. Estefania, Bacolod City', 'REGION VI', '(034) 434-1234', 'fortunetowne@chmsu.edu.ph', FALSE, TRUE),
    ('BINALBAGAN', '06003', 'Binalbagan Campus', 'Brgy. Enclaro, Binalbagan, Negros Occidental', 'REGION VI', '(034) 388-5678', 'binalbagan@chmsu.edu.ph', FALSE, TRUE)
AS new_camp ON DUPLICATE KEY UPDATE name = new_camp.name, is_main = new_camp.is_main;

-- Ensure all 8 academic departments (Colleges) exist across campuses
INSERT INTO departments (campus_id, code, name, type, parent_department_id, is_active)
VALUES 
    ((SELECT id FROM campuses WHERE code = 'TALISAY' LIMIT 1), 'CAS', 'College of Arts and Sciences', 'COLLEGE', NULL, TRUE),
    ((SELECT id FROM campuses WHERE code = 'TALISAY' LIMIT 1), 'CCS', 'College of Computer Studies', 'COLLEGE', NULL, TRUE),
    ((SELECT id FROM campuses WHERE code = 'TALISAY' LIMIT 1), 'COED', 'College of Education', 'COLLEGE', NULL, TRUE),
    ((SELECT id FROM campuses WHERE code = 'TALISAY' LIMIT 1), 'COE', 'College of Engineering', 'COLLEGE', NULL, TRUE),
    ((SELECT id FROM campuses WHERE code = 'FORTUNE_TOWNE' LIMIT 1), 'CBMA', 'College of Business Management and Accountancy', 'COLLEGE', NULL, TRUE),
    ((SELECT id FROM campuses WHERE code = 'ALIJIS' LIMIT 1), 'CIT', 'College of Industrial Technology', 'COLLEGE', NULL, TRUE),
    ((SELECT id FROM campuses WHERE code = 'BINALBAGAN' LIMIT 1), 'CCJ', 'College of Criminal Justice', 'COLLEGE', NULL, TRUE),
    ((SELECT id FROM campuses WHERE code = 'BINALBAGAN' LIMIT 1), 'COF', 'College of Fisheries', 'COLLEGE', NULL, TRUE)
AS new_dept ON DUPLICATE KEY UPDATE name = new_dept.name, type = new_dept.type;

-- ============================================================================
-- 2. ACADEMIC PROGRAMS & MAJORS INVENTORY (CHMSU)
-- ============================================================================

INSERT INTO programs (department_id, college_id, code, name, major, degree_level, ched_cmo_reference, government_permit, total_units_required, is_active)
VALUES
    -- CAS
    ((SELECT id FROM departments WHERE code = 'CAS' LIMIT 1), (SELECT id FROM departments WHERE code = 'CAS' LIMIT 1), 'BA-EL', 'Bachelor of Arts in English Language', 'Generalist', 'UNDERGRADUATE', 'CMO No. 48 s. 2017', 'GP-BAEL-2020-001', 138, TRUE),
    ((SELECT id FROM departments WHERE code = 'CAS' LIMIT 1), (SELECT id FROM departments WHERE code = 'CAS' LIMIT 1), 'BA-SS', 'Bachelor of Arts in Social Science', 'Generalist', 'UNDERGRADUATE', 'CHED PSG', 'GP-BASS-2020-002', 138, TRUE),
    ((SELECT id FROM departments WHERE code = 'CAS' LIMIT 1), (SELECT id FROM departments WHERE code = 'CAS' LIMIT 1), 'BPA', 'Bachelor of Public Administration', 'Public Policy & Governance', 'UNDERGRADUATE', 'CMO No. 06 s. 2010', 'GP-BPA-2020-003', 140, TRUE),
    ((SELECT id FROM departments WHERE code = 'CAS' LIMIT 1), (SELECT id FROM departments WHERE code = 'CAS' LIMIT 1), 'BS-AM', 'Bachelor of Science in Applied Mathematics', 'Pure & Industrial Math', 'UNDERGRADUATE', 'CMO No. 19 s. 2022', 'GP-BSAM-2020-004', 145, TRUE),
    ((SELECT id FROM departments WHERE code = 'CAS' LIMIT 1), (SELECT id FROM departments WHERE code = 'CAS' LIMIT 1), 'BS-PSYCH', 'Bachelor of Science in Psychology', 'Clinical & Industrial Psych', 'UNDERGRADUATE', 'CMO No. 34 s. 2017', 'GP-BSPSY-2020-005', 142, TRUE),
    -- CCS
    ((SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), 'BSCS', 'Bachelor of Science in Computer Science', 'Software Engineering & Data Science', 'UNDERGRADUATE', 'CMO No. 25 s. 2015', 'GP-BSCS-2020-001', 140, TRUE),
    ((SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), 'BSIT', 'Bachelor of Science in Information Technology', 'Systems & Network Admin', 'UNDERGRADUATE', 'CMO No. 25 s. 2015', 'GP-BSIT-2020-002', 146, TRUE),
    ((SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), 'BSIS', 'Bachelor of Science in Information Systems', 'Enterprise Process Architecture', 'UNDERGRADUATE', 'CMO No. 25 s. 2015', 'GP-BSIS-2020-003', 142, TRUE),
    -- COED
    ((SELECT id FROM departments WHERE code = 'COED' LIMIT 1), (SELECT id FROM departments WHERE code = 'COED' LIMIT 1), 'BSEd', 'Bachelor of Secondary Education', NULL, 'UNDERGRADUATE', 'CMO No. 75 s. 2017', 'GP-BSED-2020-010', 152, TRUE),
    ((SELECT id FROM departments WHERE code = 'COED' LIMIT 1), (SELECT id FROM departments WHERE code = 'COED' LIMIT 1), 'BEEd', 'Bachelor of Elementary Education', 'General Elementary Content', 'UNDERGRADUATE', 'CMO No. 74 s. 2017', 'GP-BEED-2020-012', 148, TRUE),
    ((SELECT id FROM departments WHERE code = 'COED' LIMIT 1), (SELECT id FROM departments WHERE code = 'COED' LIMIT 1), 'BECEd', 'Bachelor of Early Childhood Education', 'Early Development', 'UNDERGRADUATE', 'CMO No. 76 s. 2017', 'GP-BECED-2020-013', 146, TRUE),
    ((SELECT id FROM departments WHERE code = 'COED' LIMIT 1), (SELECT id FROM departments WHERE code = 'COED' LIMIT 1), 'BSNEd', 'Bachelor of Special Needs Education', 'Generalist Special Education', 'UNDERGRADUATE', 'CMO No. 77 s. 2017', 'GP-BSNED-2020-014', 148, TRUE),
    ((SELECT id FROM departments WHERE code = 'COED' LIMIT 1), (SELECT id FROM departments WHERE code = 'COED' LIMIT 1), 'BPEd', 'Bachelor of Physical Education', 'School PE & Sports Coaching', 'UNDERGRADUATE', 'CMO No. 80 s. 2017', 'GP-BPED-2020-015', 144, TRUE),
    ((SELECT id FROM departments WHERE code = 'COED' LIMIT 1), (SELECT id FROM departments WHERE code = 'COED' LIMIT 1), 'BTLEd', 'Bachelor of Technology and Livelihood Education', NULL, 'UNDERGRADUATE', 'CMO No. 78 s. 2017', 'GP-BTLED-2020-016', 150, TRUE),
    ((SELECT id FROM departments WHERE code = 'COED' LIMIT 1), (SELECT id FROM departments WHERE code = 'COED' LIMIT 1), 'BTVTEd', 'Bachelor of Technical-Vocational Teacher Education', NULL, 'UNDERGRADUATE', 'CMO No. 79 s. 2017', 'GP-BTVTED-2020-017', 150, TRUE),
    -- COE
    ((SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), 'BSCE', 'Bachelor of Science in Civil Engineering', 'Structural & Water Engineering', 'UNDERGRADUATE', 'CMO No. 92 s. 2017', 'GP-BSCV-2020-006', 168, TRUE),
    ((SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), 'BSME', 'Bachelor of Science in Mechanical Engineering', 'Machine Design & Thermal Energy', 'UNDERGRADUATE', 'CMO No. 97 s. 2017', 'GP-BSME-2020-018', 169, TRUE),
    ((SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), 'BSEE', 'Bachelor of Science in Electrical Engineering', 'Power Systems & Automation', 'UNDERGRADUATE', 'CMO No. 88 s. 2017', 'GP-BSEE-2020-019', 165, TRUE),
    ((SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), 'BSECE', 'Bachelor of Science in Electronics Engineering', 'Telecommunications & Control', 'UNDERGRADUATE', 'CMO No. 101 s. 2017', 'GP-BECE-2020-005', 166, TRUE),
    ((SELECT id FROM departments WHERE code = 'COE' LIMIT 1), (SELECT id FROM departments WHERE code = 'COE' LIMIT 1), 'BSCpE', 'Bachelor of Science in Computer Engineering', 'Embedded Systems & IoT', 'UNDERGRADUATE', 'CMO No. 87 s. 2017', 'GP-BSCE-2020-004', 162, TRUE),
    -- CBMA
    ((SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), (SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), 'BSA', 'Bachelor of Science in Accountancy', 'Professional Public Accounting', 'UNDERGRADUATE', 'CMO No. 27 s. 2017', 'GP-BSA-2020-008', 172, TRUE),
    ((SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), (SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), 'BSMA', 'Bachelor of Science in Management Accounting', 'Strategic Cost & Management', 'UNDERGRADUATE', 'CMO No. 27 s. 2017', 'GP-BSMA-2020-020', 150, TRUE),
    ((SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), (SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), 'BSBA', 'Bachelor of Science in Business Administration', NULL, 'UNDERGRADUATE', 'CMO No. 28 s. 2017', 'GP-BSBA-2020-007', 144, TRUE),
    ((SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), (SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), 'BS-ENT', 'Bachelor of Science in Entrepreneurship', 'Innovation & Enterprise Setup', 'UNDERGRADUATE', 'CMO No. 17 s. 2017', 'GP-BSENT-2020-021', 140, TRUE),
    ((SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), (SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), 'BSHM', 'Bachelor of Science in Hospitality Management', 'Culinary & Hotel Operations', 'UNDERGRADUATE', 'CMO No. 62 s. 2017', 'GP-BSHM-2020-009', 140, TRUE),
    ((SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), (SELECT id FROM departments WHERE code = 'CBMA' LIMIT 1), 'BSOA', 'Bachelor of Science in Office Administration', 'Corporate Executive Secretarial', 'UNDERGRADUATE', 'CMO No. 18 s. 2017', 'GP-BSOA-2020-022', 138, TRUE),
    -- CIT
    ((SELECT id FROM departments WHERE code = 'CIT' LIMIT 1), (SELECT id FROM departments WHERE code = 'CIT' LIMIT 1), 'BIT', 'Bachelor of Industrial Technology', NULL, 'UNDERGRADUATE', 'CHED / TESDA PSG', 'GP-BIT-2020-023', 148, TRUE),
    -- CCJ
    ((SELECT id FROM departments WHERE code = 'CCJ' LIMIT 1), (SELECT id FROM departments WHERE code = 'CCJ' LIMIT 1), 'BSCRIM', 'Bachelor of Science in Criminology', 'Forensics, Law & Corrections', 'UNDERGRADUATE', 'CMO No. 05 s. 2018', 'GP-BCRM-2020-013', 158, TRUE),
    -- COF
    ((SELECT id FROM departments WHERE code = 'COF' LIMIT 1), (SELECT id FROM departments WHERE code = 'COF' LIMIT 1), 'BSFi', 'Bachelor of Science in Fisheries', 'Aquaculture & Post-Harvest', 'UNDERGRADUATE', 'CMO No. 43 s. 2006', 'GP-BSFI-2020-024', 145, TRUE)
AS new_p ON DUPLICATE KEY UPDATE name = new_p.name, total_units_required = new_p.total_units_required;

-- Majors Table Seeding
INSERT INTO majors (program_id, code, name, description, is_active)
VALUES
    -- BSEd
    ((SELECT id FROM programs WHERE code = 'BSEd' LIMIT 1), 'MATH', 'Mathematics', 'Secondary Mathematics Teaching Specialization', TRUE),
    ((SELECT id FROM programs WHERE code = 'BSEd' LIMIT 1), 'ENG', 'English', 'English Language Teaching Specialization', TRUE),
    ((SELECT id FROM programs WHERE code = 'BSEd' LIMIT 1), 'SCI', 'General Science', 'General Science Education Specialization', TRUE),
    ((SELECT id FROM programs WHERE code = 'BSEd' LIMIT 1), 'FIL', 'Filipino', 'Pagtuturo ng Filipino Specialization', TRUE),
    ((SELECT id FROM programs WHERE code = 'BSEd' LIMIT 1), 'SS', 'Social Studies', 'Social Studies Teaching Specialization', TRUE),
    -- BTLEd
    ((SELECT id FROM programs WHERE code = 'BTLEd' LIMIT 1), 'HE', 'Home Economics', 'Home Economics and Livelihood Education Specialization', TRUE),
    ((SELECT id FROM programs WHERE code = 'BTLEd' LIMIT 1), 'IA', 'Industrial Arts', 'Industrial Arts Technology Education Specialization', TRUE),
    ((SELECT id FROM programs WHERE code = 'BTLEd' LIMIT 1), 'ICT', 'Information and Communication Technology', 'Information and Communication Technology Education', TRUE),
    -- BTVTEd
    ((SELECT id FROM programs WHERE code = 'BTVTEd' LIMIT 1), 'ET', 'Electrical Technology', 'Electrical Technology Teacher Education Specialization', TRUE),
    ((SELECT id FROM programs WHERE code = 'BTVTEd' LIMIT 1), 'ELX', 'Electronics Technology', 'Electronics Technology Teacher Education Specialization', TRUE),
    ((SELECT id FROM programs WHERE code = 'BTVTEd' LIMIT 1), 'FSM', 'Food and Service Management', 'Food and Service Management Teacher Education', TRUE),
    -- BSBA
    ((SELECT id FROM programs WHERE code = 'BSBA' LIMIT 1), 'FM', 'Financial Management', 'Corporate Finance, Banking and Investments Specialization', TRUE),
    ((SELECT id FROM programs WHERE code = 'BSBA' LIMIT 1), 'MM', 'Marketing Management', 'Marketing Research, Brand Management and Consumer Behavior', TRUE),
    ((SELECT id FROM programs WHERE code = 'BSBA' LIMIT 1), 'HRM', 'Human Resource Management', 'Human Resource Planning, Recruitment, Training and Labor Relations', TRUE),
    -- BIT
    ((SELECT id FROM programs WHERE code = 'BIT' LIMIT 1), 'AUTO', 'Automotive Technology', 'Automotive Power Trains and Tune-up Specialization', TRUE),
    ((SELECT id FROM programs WHERE code = 'BIT' LIMIT 1), 'COMP', 'Computer Technology', 'Computer Hardware, Network Systems and Diagnostics', TRUE),
    ((SELECT id FROM programs WHERE code = 'BIT' LIMIT 1), 'ELEC', 'Electrical Technology', 'Electrical Power, Wiring, Control and Industrial Systems', TRUE),
    ((SELECT id FROM programs WHERE code = 'BIT' LIMIT 1), 'ELX', 'Electronics Technology', 'Consumer and Industrial Electronics, Microcontrollers', TRUE),
    ((SELECT id FROM programs WHERE code = 'BIT' LIMIT 1), 'MECH', 'Mechanical Technology', 'Machining, Metal Fabrication, Welding and Hydraulics', TRUE),
    ((SELECT id FROM programs WHERE code = 'BIT' LIMIT 1), 'DRAFT', 'Architectural Drafting', 'CADD, Architectural Planning and Building Modeling', TRUE),
    ((SELECT id FROM programs WHERE code = 'BIT' LIMIT 1), 'HVAC', 'HVAC/R Technology', 'Heating, Ventilation, Air Conditioning and Refrigeration', TRUE),
    ((SELECT id FROM programs WHERE code = 'BIT' LIMIT 1), 'CUL', 'Culinary Technology', 'Commercial Cookery, Food Safety and Culinary Production', TRUE),
    ((SELECT id FROM programs WHERE code = 'BIT' LIMIT 1), 'FASH', 'Fashion and Apparel', 'Apparel Design, Production and Garments Technology', TRUE)
AS new_m ON DUPLICATE KEY UPDATE name = new_m.name, description = new_m.description;

-- ============================================================================
-- 3. MASTER COURSE CATALOG SEEDING (579 COURSES)
-- ============================================================================

INSERT INTO courses (code, title, lecture_units, lab_units, credit_units, contact_hours_lec, contact_hours_lab, category, description, is_active)
VALUES
    ('GEC-UTS', 'Understanding the Self', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Nature of identity, factors affecting personal identity.', TRUE),
    ('GEC-RPH', 'Readings in Philippine History', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Philippine history from selected primary sources.', TRUE),
    ('GEC-TCW', 'The Contemporary World', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Globalization and its economic, political, and cultural impact.', TRUE),
    ('GEC-MMW', 'Mathematics in the Modern World', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Nature of math, practical, intellectual, and aesthetic dimensions.', TRUE),
    ('GEC-PC', 'Purposive Communication', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Writing, speaking, and presenting to diverse audiences.', TRUE),
    ('GEC-ART', 'Art Appreciation', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Visual, auditory, and performing arts analysis and critique.', TRUE),
    ('GEC-STS', 'Science, Technology, and Society', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Interactions between science, technology, and social contexts.', TRUE),
    ('GEC-ETH', 'Ethics', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION', 'Principles of ethical behavior in modern society.', TRUE),
    ('RIZAL', 'Life and Works of Rizal', 3.00, 0.00, 3.00, 3, 0, 'MANDATED_COURSE', 'Mandated by RA 1425: Study of Jose Rizal.', TRUE),
    ('GEC-ELE1', 'Living in the IT Era', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION_ELECTIVE', 'Evolution and societal impact of digital computing.', TRUE),
    ('GEC-ELE2', 'Philippine Popular Culture', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION_ELECTIVE', 'Analysis of contemporary Filipino media, arts, and traditions.', TRUE),
    ('GEC-ELE3', 'Environmental Science', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION_ELECTIVE', 'Ecology, sustainable resource use, and environmental policies.', TRUE),
    ('GEC-ELE4', 'Gender and Society', 3.00, 0.00, 3.00, 3, 0, 'GENERAL_EDUCATION_ELECTIVE', 'Gender perspectives, equity, and societal expectations.', TRUE),
    ('PE-1', 'Movement Competency Training (PATHFit 1)', 2.00, 0.00, 2.00, 2, 0, 'PHYSICAL_EDUCATION', 'Fundamental movement patterns and core biomechanics.', TRUE),
    ('PE-2', 'Exercise-based Fitness Activities (PATHFit 2)', 2.00, 0.00, 2.00, 2, 0, 'PHYSICAL_EDUCATION', 'Cardiovascular endurance, resistance training, and fitness.', TRUE),
    ('PE-3', 'Choice of Dance / Sports (PATHFit 3)', 2.00, 0.00, 2.00, 2, 0, 'PHYSICAL_EDUCATION', 'Rhythmic activities, dance fundamentals, and recreational games.', TRUE),
    ('PE-4', 'Choice of Team / Martial Sports (PATHFit 4)', 2.00, 0.00, 2.00, 2, 0, 'PHYSICAL_EDUCATION', 'Team sports dynamics, tactical skills, and martial sports.', TRUE),
    ('NSTP-1', 'National Service Training Program 1', 3.00, 0.00, 3.00, 3, 0, 'MANDATED_COURSE', 'Mandated by RA 9163: Civic welfare and literacy service.', TRUE),
    ('NSTP-2', 'National Service Training Program 2', 3.00, 0.00, 3.00, 3, 0, 'MANDATED_COURSE', 'Mandated by RA 9163: Community immersion project execution.', TRUE),
    ('CC101', 'Introduction to Computing', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'Computing principles, binary logic, algorithms, and architectures.', TRUE),
    ('CC102', 'Computer Programming 1 (Structured Programming)', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'Control flows, arrays, functions, pointers, and structured C/Java.', TRUE),
    ('CC103', 'Computer Programming 2 (Object-Oriented Programming)', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'OOP principles: encapsulation, polymorphism, inheritance, and exceptions.', TRUE),
    ('CC104', 'Data Structures and Algorithms', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'Stacks, queues, linked lists, trees, graphs, sorting, and Big-O.', TRUE),
    ('CC105', 'Information Management & Database Systems', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'Relational data modeling, SQL queries, ER diagrams, 3NF, and ACID.', TRUE),
    ('CC106', 'Applications Development & Emerging Technologies', 2.00, 1.00, 3.00, 2, 3, 'INSTITUTIONAL_CORE', 'Full-stack reactive web apps, microservices, cloud APIs.', TRUE),
    ('IT-DISC', 'Discrete Mathematics for IT', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Logic, set theory, relations, combinatorics, and graph theory.', TRUE),
    ('IT-NET1', 'Networking 1 (Fundamentals & IP Routing)', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'OSI model, TCP/IP, IP addressing, subnetting, and switching.', TRUE),
    ('IT-NET2', 'Networking 2 (Advanced Switching & WAN)', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'VLANs, inter-VLAN routing, OSPF, ACLs, NAT, and WAN technologies.', TRUE),
    ('IT-WEB1', 'Web Systems and Technologies 1', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Client-side reactive frontend, modern JS/TS, HTML5/CSS3.', TRUE),
    ('IT-WEB2', 'Web Systems and Technologies 2 (Enterprise Full-Stack)', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Enterprise backend architectures, Spring Boot, REST APIs, ORM.', TRUE),
    ('IT-SYSADM', 'Systems Administration and Maintenance', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Linux server administration, shell scripting, services, containers.', TRUE),
    ('IT-IAS1', 'Information Assurance and Security 1', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Security fundamentals, cryptography, PKI, firewall configurations.', TRUE),
    ('IT-IAS2', 'Information Assurance and Security 2', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Vulnerability assessment, ethical hacking, OWASP Top 10 defenses.', TRUE),
    ('IT-INT', 'Integrative Programming and Technologies', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Middleware, message queues, ETL pipelines, data exchange formats.', TRUE),
    ('IT-MOB', 'Mobile Application Development', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Native and cross-platform mobile application development.', TRUE),
    ('IT-CAP1', 'IT Capstone Project 1', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Project proposal, systems requirements, design, and defense.', TRUE),
    ('IT-CAP2', 'IT Capstone Project 2', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Full system implementation, user evaluation, and colloquium.', TRUE),
    ('IT-PROF', 'Social and Professional Issues in IT', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Professional ethics, cyberlaw, intellectual property, data privacy.', TRUE),
    ('IT-PRAC', 'IT Industry Internship (486 Hours)', 0.00, 6.00, 6.00, 0, 18, 'PRACTICUM', 'Supervised industry on-the-job training in computing enterprise.', TRUE),
    ('IT-CLOUD', 'Cloud Computing & Serverless Architecture', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'AWS, Azure, virtualization, microservices, containerization.', TRUE),
    ('IT-DATA', 'Data Warehousing & Business Intelligence', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'OLAP, ETL, dimensional modeling, Tableau, PowerBI.', TRUE),
    ('IT-AI', 'Applied Artificial Intelligence & Machine Learning', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Supervised learning, neural networks, computer vision, NLP.', TRUE),
    ('IS-FUND', 'Fundamentals of Information Systems', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Role of IS in modern organizations, digital transformation.', TRUE),
    ('IS-EA', 'Enterprise Architecture', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'TOGAF framework, enterprise modeling, capability alignment.', TRUE),
    ('IS-BPM', 'Business Process Management', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'BPMN notation, process modeling, re-engineering, optimization.', TRUE),
    ('IS-PROJ', 'IS Project Management', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'PMBOK, agile management, risk analysis, and scheduling.', TRUE),
    ('IS-STRAT', 'IS Strategy, Governance, and Sourcing', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'IT governance frameworks, COBIT, strategic planning, outsourcing.', TRUE),
    ('IS-SAD', 'Systems Analysis and Design', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'UML modeling, requirements elicitation, design specifications.', TRUE),
    ('IS-AUDIT', 'IS Audit and Risk Management', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'CISA principles, IT controls evaluation, compliance audits.', TRUE),
    ('IS-ERP', 'Enterprise Resource Planning Systems', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'SAP/Odoo implementation, business modules, workflow automation.', TRUE),
    ('MATH-ENG1', 'Differential Calculus for Engineers', 3.00, 0.00, 3.00, 3, 0, 'ENGINEERING_MATH', 'Limits, continuity, differentiation techniques, curve sketching.', TRUE),
    ('MATH-ENG2', 'Integral Calculus for Engineers', 3.00, 0.00, 3.00, 3, 0, 'ENGINEERING_MATH', 'Definite/indefinite integrals, techniques of integration, volumes.', TRUE),
    ('MATH-DIFF', 'Differential Equations', 3.00, 0.00, 3.00, 3, 0, 'ENGINEERING_MATH', 'First and higher order ODEs, Laplace transforms, power series.', TRUE),
    ('ENG-CHEM', 'Chemistry for Engineers with Lab', 3.00, 1.00, 4.00, 3, 3, 'ENGINEERING_SCIENCE', 'Chemical principles, materials science, electrochemistry, polymers.', TRUE),
    ('ENG-PHYS1', 'Physics 1 for Engineers with Lab', 3.00, 1.00, 4.00, 3, 3, 'ENGINEERING_SCIENCE', 'Mechanics, Newton laws, work-energy, momentum, rotational motion.', TRUE),
    ('ENG-PHYS2', 'Physics 2 for Engineers with Lab', 3.00, 1.00, 4.00, 3, 3, 'ENGINEERING_SCIENCE', 'Electricity, magnetism, optics, electromagnetic induction.', TRUE),
    ('ENG-STAT', 'Statics of Rigid Bodies', 3.00, 0.00, 3.00, 3, 0, 'ENGINEERING_SCIENCE', 'Force systems, equilibrium, trusses, centroids, moments of inertia.', TRUE),
    ('ENG-DYN', 'Dynamics of Rigid Bodies', 3.00, 0.00, 3.00, 3, 0, 'ENGINEERING_SCIENCE', 'Kinematics/kinetics of particles and rigid bodies, impulse-momentum.', TRUE),
    ('ENG-DEFORM', 'Mechanics of Deformable Bodies', 3.00, 0.00, 3.00, 3, 0, 'ENGINEERING_SCIENCE', 'Stress-strain, axial loading, torsion, flexural stresses, deflections.', TRUE),
    ('ENG-ECON', 'Engineering Economics', 3.00, 0.00, 3.00, 3, 0, 'ENGINEERING_SCIENCE', 'Time value of money, cash flow diagrams, depreciation, ROI.', TRUE),
    ('ENG-MGT', 'Engineering Management', 3.00, 0.00, 3.00, 3, 0, 'ENGINEERING_SCIENCE', 'Organizational structures, operations, quality systems in engg.', TRUE),
    ('ENG-TECH', 'Technopreneurship 101', 3.00, 0.00, 3.00, 3, 0, 'ENGINEERING_SCIENCE', 'Technology business models, intellectual property, pitching.', TRUE),
    ('CE-SURV1', 'Elementary Surveying with Fieldwork', 2.00, 2.00, 4.00, 2, 6, 'PROFESSIONAL_MAJOR', 'Distance measurement, leveling, traverses, topographic mapping.', TRUE),
    ('CE-SURV2', 'Higher Surveying with Fieldwork', 2.00, 2.00, 4.00, 2, 6, 'PROFESSIONAL_MAJOR', 'Triangulation, global positioning systems, hydrographic surveying.', TRUE),
    ('CE-STRUCT1', 'Structural Theory 1', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Determinate structures, influence lines, deflection analysis.', TRUE),
    ('CE-STRUCT2', 'Structural Theory 2', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Indeterminate structures, slope-deflection, moment distribution.', TRUE),
    ('CE-HYDRO', 'Hydraulics and Fluid Mechanics', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Fluid statics/dynamics, flow in closed conduits and open channels.', TRUE),
    ('CE-GEOTECH', 'Geotechnical Engineering (Soil Mechanics)', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Soil classification, permeability, shear strength, foundations.', TRUE),
    ('CE-CONC', 'Reinforced Concrete Design', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Working stress, ultimate strength design of beams, slabs, columns.', TRUE),
    ('CE-STEEL', 'Design of Steel Structures', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Structural steel members, tension, compression, connections.', TRUE),
    ('CE-HIGHWAY', 'Highway and Railroad Engineering', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Geometric design of highways, pavement design, traffic control.', TRUE),
    ('CE-WATER', 'Water Resources & Environmental Engineering', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Hydrology, water supply distribution, sewerage treatment systems.', TRUE),
    ('CE-CONST', 'Construction Methods & Project Management', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'CPM/PERT, equipment scheduling, construction contracts & specifications.', TRUE),
    ('CE-FOUND', 'Foundation & Earth Retaining Design', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Shallow/deep foundations, pile groups, sheet pile walls, slope stability.', TRUE),
    ('CE-DES1', 'Civil Engineering Capstone Design 1', 1.00, 2.00, 3.00, 1, 6, 'CAPSTONE', 'Comprehensive civil engineering design proposal and defense.', TRUE),
    ('CE-DES2', 'Civil Engineering Capstone Design 2', 0.00, 2.00, 2.00, 0, 6, 'CAPSTONE', 'Final design report, environmental impact analysis, colloquium.', TRUE),
    ('CE-OJT', 'CE Field Immersion / OJT (300 Hours)', 0.00, 3.00, 3.00, 0, 9, 'PRACTICUM', 'Supervised on-site construction engineering field training.', TRUE),
    ('ME-THERMO1', 'Thermodynamics 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'First and second laws, properties of pure substances, ideal gas.', TRUE),
    ('ME-THERMO2', 'Thermodynamics 2', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Vapor/gas power cycles, refrigeration cycles, psychrometrics.', TRUE),
    ('ME-MACH1', 'Machine Design 1', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Design of machine elements: shafts, keys, couplings, fasteners.', TRUE),
    ('ME-MACH2', 'Machine Design 2', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Gears, belts, clutches, brakes, flywheels, pressure vessels.', TRUE),
    ('ME-FLUID', 'Fluid Mechanics for Mechanical Engineers', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Fluid kinematics, Navier-Stokes, boundary layer theory, turbomachinery.', TRUE),
    ('ME-HEAT', 'Heat Transfer Operations', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Conduction, convection, radiation, heat exchanger design.', TRUE),
    ('ME-POWER', 'Power Plant Engineering', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Steam, gas, diesel, hydro, and geothermal energy generation plants.', TRUE),
    ('ME-RAC', 'Refrigeration and Air Conditioning Systems', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Cooling load calculations, duct design, chiller operations.', TRUE),
    ('ME-MAT', 'Materials Science and Engineering for ME', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Phase diagrams, heat treatment, ferrous/non-ferrous alloys, corrosion.', TRUE),
    ('ME-MFG', 'Manufacturing and Machine Tool Processes', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Casting, forming, CNC machining, additive manufacturing.', TRUE),
    ('ME-AUTO', 'Automotive Engineering & Internal Combustion Engines', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Otto/Diesel cycle analysis, combustion dynamics, emissions control.', TRUE),
    ('ME-DES1', 'ME Capstone Project & Machine Design 1', 1.00, 2.00, 3.00, 1, 6, 'CAPSTONE', 'Mechanical engineering systems proposal and technical feasibility.', TRUE),
    ('ME-DES2', 'ME Capstone Project & Machine Design 2', 0.00, 2.00, 2.00, 0, 6, 'CAPSTONE', 'Prototyping, testing, computational simulation, oral defense.', TRUE),
    ('ME-OJT', 'ME Supervised Industrial Internship (300 Hours)', 0.00, 3.00, 3.00, 0, 9, 'PRACTICUM', 'Supervised manufacturing or power plant engineering internship.', TRUE),
    ('EE-CIRC1', 'Electrical Circuits 1 with Lab', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'DC circuits, Ohm law, Kirchhoff laws, network theorems, transient.', TRUE),
    ('EE-CIRC2', 'Electrical Circuits 2 with Lab', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'AC circuits, phasors, resonant circuits, balanced/unbalanced 3-phase.', TRUE),
    ('EE-MACH1', 'Electrical Machines 1 (DC Machines & Transformers)', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'DC generators, motors, single/three-phase transformers, testing.', TRUE),
    ('EE-MACH2', 'Electrical Machines 2 (AC Synchronous & Induction)', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Alternators, synchronous motors, polyphase induction motors.', TRUE),
    ('EE-POWER1', 'Electrical Power Systems Analysis 1', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Transmission line modeling, bus admittance matrices, Gauss-Seidel.', TRUE),
    ('EE-POWER2', 'Electrical Power Systems Analysis 2 (Faults & Stability)', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Symmetrical components, transient stability, relay protection.', TRUE),
    ('EE-DISTRIB', 'Distribution Systems & Substation Design', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Substation layout, feeder loading, voltage drops, power factor.', TRUE),
    ('EE-ILLUM', 'Illumination Engineering & Interior Electrical Design', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Lighting metrics, PEC branch circuits, emergency power systems.', TRUE),
    ('EE-CONTROL', 'Feedback and Industrial Control Systems', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Root locus, Bode plots, PID controllers, PLC ladder logic.', TRUE),
    ('EE-CODE', 'Philippine Electrical Code & Safety Standards', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'PEC Articles, conductor ampacities, grounding, arc-flash safety.', TRUE),
    ('EE-DES1', 'EE Capstone Design 1', 1.00, 2.00, 3.00, 1, 6, 'CAPSTONE', 'Power system or automation engineering design proposal.', TRUE),
    ('EE-DES2', 'EE Capstone Design 2', 0.00, 2.00, 2.00, 0, 6, 'CAPSTONE', 'Hardware-software implementation, testing, defense.', TRUE),
    ('EE-OJT', 'EE Field Immersion / OJT (300 Hours)', 0.00, 3.00, 3.00, 0, 9, 'PRACTICUM', 'Supervised training with power utility, grid operator, or plant.', TRUE),
    ('ECE-ELEC1', 'Electronic Devices and Circuit Analysis', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Diodes, BJTs, FETs, operational amplifiers, circuit simulations.', TRUE),
    ('ECE-ELEC2', 'Electronic Circuit Design & Applications', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Power amplifiers, active filters, multivibrators, regulators.', TRUE),
    ('ECE-COMM1', 'Principles of Communication Systems', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'AM, FM, PM, noise analysis, modulators, demodulators.', TRUE),
    ('ECE-COMM2', 'Digital Communications & Telecommunications', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'ASK, FSK, PSK, QAM, PCM, cellular topologies, optical fiber.', TRUE),
    ('ECE-SIGNALS', 'Signals, Spectra, and Signal Processing', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Fourier/Z-transforms, DFT, FFT, FIR/IIR filter synthesis.', TRUE),
    ('ECE-EMAG', 'Electromagnetics & Transmission Lines', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Maxwell equations, wave propagation, Smith charts, impedance matching.', TRUE),
    ('ECE-WIRELESS', 'Wireless and Satellite Communication Systems', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Link budgets, orbital mechanics, transponders, 5G architectures.', TRUE),
    ('ECE-LAWS', 'ECE Laws, Contracts, Ethics & Safety', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'RA 9292, NTC regulations, frequency spectrum allocation.', TRUE),
    ('ECE-DES1', 'ECE Capstone Project 1', 1.00, 2.00, 3.00, 1, 6, 'CAPSTONE', 'Telecommunication or signal processing system engineering design.', TRUE),
    ('ECE-DES2', 'ECE Capstone Project 2', 0.00, 2.00, 2.00, 0, 6, 'CAPSTONE', 'Prototyping, calibration, performance verification, defense.', TRUE),
    ('ECE-OJT', 'ECE Industry Practicum (300 Hours)', 0.00, 3.00, 3.00, 0, 9, 'PRACTICUM', 'Supervised telecommunications or semiconductor industry training.', TRUE),
    ('CPE-LOGIC', 'Logic Circuits and Design', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Combinational/sequential logic, flip-flops, counters, FPGAs.', TRUE),
    ('CPE-ARCH', 'Computer Architecture and Organization', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Instruction set architectures, pipelining, cache memory, buses.', TRUE),
    ('CPE-EMBED', 'Embedded Systems and IoT', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Microcontrollers, ARM architecture, sensor interfacing, MQTT.', TRUE),
    ('CPE-OS', 'Operating Systems Principles & Kernel Design', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Process scheduling, deadlocks, virtual memory, device drivers.', TRUE),
    ('CPE-DSP', 'Digital Signal Processing for Computer Engineers', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Discrete systems, filtering, audio and video signal processing.', TRUE),
    ('CPE-NET', 'Data Communications & Computer Networks', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'LAN/WAN protocols, network simulation, socket programming.', TRUE),
    ('CPE-SEC', 'Embedded Systems Security & Cryptography', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Hardware security, side-channel attacks, secure bootloader.', TRUE),
    ('CPE-DES1', 'Computer Engineering Capstone Design 1', 1.00, 2.00, 3.00, 1, 6, 'CAPSTONE', 'IoT, robotics, or hardware-software system design proposal.', TRUE),
    ('CPE-DES2', 'Computer Engineering Capstone Design 2', 0.00, 2.00, 2.00, 0, 6, 'CAPSTONE', 'System prototyping, field deployment, benchmark evaluation, defense.', TRUE),
    ('CPE-OJT', 'CpE Supervised Field Training (300 Hours)', 0.00, 3.00, 3.00, 0, 9, 'PRACTICUM', 'Supervised embedded systems, semiconductor, or IT engineering training.', TRUE),
    ('ED-CHILD', 'Child & Adolescent Learners & Learning Principles', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_EDUCATION', 'Physical, cognitive, socio-emotional development of learners.', TRUE),
    ('ED-TEACH', 'Facilitating Learner-Centered Teaching', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_EDUCATION', 'Learning theories, motivation, differentiated instructional design.', TRUE),
    ('ED-SPED', 'Foundations of Special and Inclusive Education', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_EDUCATION', 'Inclusive classroom environments, adapting curricula for disabilities.', TRUE),
    ('ED-ASS1', 'Assessment in Learning 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_EDUCATION', 'Traditional cognitive assessment, test construction, psychometrics.', TRUE),
    ('ED-ASS2', 'Assessment in Learning 2', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_EDUCATION', 'Authentic assessment, rubrics, portfolio assessment, grading.', TRUE),
    ('ED-CURR', 'The Teacher and the School Curriculum', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_EDUCATION', 'Curriculum models, implementation, evaluation, reform policies.', TRUE),
    ('ED-COMM', 'The Teacher and the Community, School Leadership', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_EDUCATION', 'Educational leadership, community partnerships, organizational ethics.', TRUE),
    ('ED-TTL1', 'Technology for Teaching and Learning 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_EDUCATION', 'ICT integration in education, multimedia materials development.', TRUE),
    ('ED-TTL2', 'Technology for Teaching and Learning 2', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_EDUCATION', 'Subject-specific digital learning technologies and instructional tools.', TRUE),
    ('ED-LIT', 'Building and Enhancing New Literacies', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_EDUCATION', 'Multicultural, digital, media, financial, and ecological literacies.', TRUE),
    ('ED-RES', 'Action Research in Education', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_EDUCATION', 'Classroom-based action research methodology, data analysis.', TRUE),
    ('ED-FS1', 'Field Study 1: Observations of Teaching-Learning', 3.00, 0.00, 3.00, 3, 0, 'EXPERIENTIAL_LEARNING', 'School immersion, classroom observation, teacher-learner dynamics.', TRUE),
    ('ED-FS2', 'Field Study 2: Assistantship & Action Research', 3.00, 0.00, 3.00, 3, 0, 'EXPERIENTIAL_LEARNING', 'Instructional assistantship, teaching demo, action research data.', TRUE),
    ('ED-TI', 'Teaching Internship (Full-Term Student Teaching)', 6.00, 0.00, 6.00, 6, 0, 'EXPERIENTIAL_LEARNING', 'Full-semester supervised classroom student teaching immersion.', TRUE),
    ('MATH-CALC1', 'Calculus for Secondary Mathematics Teachers 1', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Pedagogical content knowledge in differential calculus.', TRUE),
    ('MATH-CALC2', 'Calculus for Secondary Mathematics Teachers 2', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Pedagogical content knowledge in integral calculus and series.', TRUE),
    ('MATH-ALG1', 'College and Advanced Algebra for Teachers', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Abstract algebraic structures, polynomial rings, group theory.', TRUE),
    ('MATH-ALG2', 'Linear Algebra for Teachers', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Vector spaces, matrices, determinants, linear transformations.', TRUE),
    ('MATH-GEOM', 'Modern and Transformational Geometry', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Euclidean/non-Euclidean geometry, coordinate transformations.', TRUE),
    ('MATH-STAT', 'Probability and Statistics for Teachers', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Probability models, descriptive and inferential statistics.', TRUE),
    ('MATH-NUM', 'Number Theory', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Divisibility, prime factorization, modular arithmetic, Diophantine.', TRUE),
    ('MATH-TRIG', 'Plane and Spherical Trigonometry', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Trigonometric identities, spherical triangles, celestial apps.', TRUE),
    ('MATH-MATHED', 'Principles & Strategies of Teaching Mathematics', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Heuristics, Polya problem solving, lesson design in high school math.', TRUE),
    ('MATH-ASSESS', 'Assessment and Evaluation in Mathematics', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Diagnostic, formative, summative math tests, performance tasks.', TRUE),
    ('MATH-DIFFEQ', 'Differential Equations for Educators', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'First-order differential equations and mathematical modeling.', TRUE),
    ('MATH-RES', 'Research in Mathematics Education', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Mathematical education research design and pedagogical findings.', TRUE),
    ('MATH-SEM', 'Seminar on Technology & Trends in Math Ed', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'GeoGebra, Desmos, contemporary trends in math pedagogy.', TRUE),
    ('ENG-LING', 'Introduction to Linguistics', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Phonology, morphology, syntax, semantics, and sociolinguistics.', TRUE),
    ('ENG-STRUK', 'Structure of English (Applied Grammar)', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Grammatical categories, structural analysis, syntactic parsing.', TRUE),
    ('ENG-LIT', 'Teaching and Assessment of Literature', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Literary criticism, pedagogy of poetry, drama, and prose.', TRUE),
    ('ENG-SPCH', 'Speech and Theater Arts', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Oral communication techniques, debate, theatrical production.', TRUE),
    ('ENG-MYTH', 'Mythology and Folklore', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'World mythologies, indigenous Philippine folklore in teaching.', TRUE),
    ('ENG-WRIT', 'Creative Writing and Composition Pedagogy', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Craft of poetry, short fiction, and essay composition instruction.', TRUE),
    ('ENG-SEM', 'Semantics and Pragmatics in English Teaching', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Meaning in context, speech act theory, communicative competencies.', TRUE),
    ('ENG-STY', 'Stylistics and Discourse Analysis', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Linguistic analysis of literary texts and media discourses.', TRUE),
    ('ENG-TRANS', 'Translation and Editing of Textual Materials', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Theory and practice of English-Filipino translation and proofreading.', TRUE),
    ('ENG-CHILD', 'Children and Adolescent Literature', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Survey of YA novels, picture books, thematic reading programs.', TRUE),
    ('ENG-REMED', 'Remedial Instruction in English', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Diagnosing reading difficulties, phonics intervention programs.', TRUE),
    ('ENG-RES', 'Research in English Language Teaching', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Second language acquisition research, literacy development.', TRUE),
    ('ENG-CAMP', 'Campus Journalism and School Paper Management', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'News writing, editorial management, layout, press ethics.', TRUE),
    ('SCI-BIO1', 'General Biology 1 with Laboratory', 3.00, 1.00, 4.00, 3, 3, 'SPECIALIZATION_MAJOR', 'Cell biology, genetics, evolutionary theory, physiology.', TRUE),
    ('SCI-BIO2', 'General Biology 2 with Laboratory', 3.00, 1.00, 4.00, 3, 3, 'SPECIALIZATION_MAJOR', 'Organismal biology, plant and animal diversity, ecology.', TRUE),
    ('SCI-CHEM1', 'General Chemistry 1 with Laboratory', 3.00, 1.00, 4.00, 3, 3, 'SPECIALIZATION_MAJOR', 'Stoichiometry, atomic structure, chemical bonding, reactions.', TRUE),
    ('SCI-CHEM2', 'General Chemistry 2 with Laboratory', 3.00, 1.00, 4.00, 3, 3, 'SPECIALIZATION_MAJOR', 'Thermodynamics, chemical equilibrium, acids and bases, kinetics.', TRUE),
    ('SCI-PHYS1', 'General Physics 1 with Laboratory', 3.00, 1.00, 4.00, 3, 3, 'SPECIALIZATION_MAJOR', 'Classical mechanics, heat, sound, and laboratory experiments.', TRUE),
    ('SCI-PHYS2', 'General Physics 2 with Laboratory', 3.00, 1.00, 4.00, 3, 3, 'SPECIALIZATION_MAJOR', 'Electricity, magnetism, optics, wave motion, modern physics.', TRUE),
    ('SCI-EARTH', 'Earth and Space Science', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Geology, meteorology, oceanography, astronomical systems.', TRUE),
    ('SCI-ASTR', 'Astronomy and Environmental Systems', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Celestial mechanics, solar system dynamics, stellar evolution.', TRUE),
    ('SCI-ECOL', 'Ecology and Conservation Biology', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Ecosystem energetics, biodiversity preservation, Philippine biomes.', TRUE),
    ('SCI-ORG', 'Organic Chemistry for Science Teachers', 3.00, 1.00, 4.00, 3, 3, 'SPECIALIZATION_MAJOR', 'Hydrocarbons, functional groups, reaction mechanisms, biomolecules.', TRUE),
    ('SCI-MICRO', 'Microbiology and Parasitology', 3.00, 1.00, 4.00, 3, 3, 'SPECIALIZATION_MAJOR', 'Bacterial genetics, culturing techniques, virology, immunology.', TRUE),
    ('SCI-GENET', 'Genetics and Molecular Biology', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Mendelian inheritance, DNA replication, gene expression, PCR.', TRUE),
    ('SCI-METHO', 'Teaching Strategies in Science Education', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Inquiry-based labs, 5E learning cycle, science process skills.', TRUE),
    ('SCI-RES', 'Research in Science Education', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Scientific investigation design, classroom experiments, defense.', TRUE),
    ('FIL-INTRO', 'Introduksyon sa Pag-aaral ng Wika', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Pundasyon ng wikang Filipino, kasaysayan at ebolusyon.', TRUE),
    ('FIL-ESTRUK', 'Estruktura ng Wikang Filipino', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Ponolohiya, morpolohiya, at sintaks ng wikang pambansa.', TRUE),
    ('FIL-PANIT', 'Panitikan ng Pilipinas', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Mga obra maestra at panitikang rehiyonal ng Pilipinas.', TRUE),
    ('FIL-RETOR', 'Masining na Pagpapahayag at Retorika', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Prinsipyo ng mabisang pagsulat at retorikang Filipino.', TRUE),
    ('FIL-WIKA', 'Ugnayan ng Wika, Kultura at Lipunan', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Sosyolinggwistika at kulturang Pilipino sa pagtuturo.', TRUE),
    ('FIL-TULA', 'Pagtuturo at Pagtataya ng Panulaan', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Elemento ng tula, tayutay, at pamamaraan ng pagtuturo.', TRUE),
    ('FIL-DULA', 'Dulaang Filipino at Produksyong Pantanghalan', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Kasaysayan ng sarswela, senakulo, at modernong dula.', TRUE),
    ('FIL-MAIKLI', 'Pagsusuri sa Maikling Kwento at Nobelang Tagalog', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Mga temang sosyo-pulitikal sa naratibong tuluyan.', TRUE),
    ('FIL-SANAY', 'Pagsulat ng Sanaysay at Iba pang Anyong Tuluyan', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Pormal at di-pormal na sanaysay, kolum, at editoryal.', TRUE),
    ('FIL-TRANS', 'Teorya at Praktika ng Pagsasaling-Wika', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Pagsasalin ng tekstong pampanitikan at siyentipiko.', TRUE),
    ('FIL-KULT', 'Kulturang Popular at Wikang Filipino', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Media, musika, pelikulang Pilipino, at wika ng kabataan.', TRUE),
    ('FIL-REMED', 'Paggamot at Pagwawasto sa Kahinaan sa Pagbasa', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Interbensyon sa pagbasa at pagsulat sa Filipino.', TRUE),
    ('FIL-SALIK', 'Pananaliksik sa Wika at Panitikan', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Metodolohiya ng pananaliksik sa wika at panitikang Filipino.', TRUE),
    ('EED-CONTENT1', 'Teaching Literacy in the Elementary Grades', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Methods and techniques in developing foundational literacy.', TRUE),
    ('EED-CONTENT2', 'Teaching Social Studies in Elementary Grades', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Pedagogy of Araling Panlipunan and civic awareness.', TRUE),
    ('EED-MOTHER', 'Mother Tongue-Based Multilingual Education', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'MTB-MLE curriculum implementation and instructional materials.', TRUE),
    ('EED-ART', 'Teaching Arts and Music in Elementary Grades', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Art and music pedagogical strategies for young learners.', TRUE),
    ('EED-MATH1', 'Teaching Mathematics in Primary Grades', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Foundational arithmetic, numeracy heuristics, manipulatives.', TRUE),
    ('EED-MATH2', 'Teaching Mathematics in Intermediate Grades', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Fractions, decimals, geometry, elementary problem-solving.', TRUE),
    ('EED-SCI1', 'Teaching Science in Elementary: Biology & Chemistry', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Inquiry-based life science experiments for elementary.', TRUE),
    ('EED-SCI2', 'Teaching Science in Elementary: Physics & Earth Sci', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Hands-on experiments in force, motion, earth systems.', TRUE),
    ('EED-ENG', 'Teaching English in Elementary Grades (Language Arts)', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Listening, speaking, reading, writing, and viewing pedagogy.', TRUE),
    ('EED-FIL', 'Pagtuturo ng Filipino sa Elementarya', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Kasanayan sa pakikinig, pagsasalita, pagbasa, at pagsulat.', TRUE),
    ('EED-VAL', 'Good Manners and Right Conduct (Edukasyon sa Pagpapakatao)', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Values formation, moral character development for children.', TRUE),
    ('EED-PE', 'Teaching Physical Education & Health in Elementary', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Gross motor games, child nutrition, personal hygiene.', TRUE),
    ('EED-RES', 'Research in Elementary Education', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Elementary classroom pedagogy action research and defense.', TRUE),
    ('ECED-DEV', 'Early Childhood Development and Psychology', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Physical, cognitive, and social development from birth to age 8.', TRUE),
    ('ECED-PLAY', 'Play and Creative Activities in Early Childhood', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Role of play in learning, creative expression, movement.', TRUE),
    ('ECED-CURR', 'Early Childhood Curriculum Models', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Montessori, Reggio Emilia, HighScope, and DepEd Kindergarten.', TRUE),
    ('ECED-LIT', 'Early Literacy and Language Development', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Emergent reading, phonological awareness, storytelling.', TRUE),
    ('ECED-NUM', 'Early Childhood Numeracy Concepts', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Foundational math concepts, manipulatives for preschool.', TRUE),
    ('ECED-INCL', 'Inclusive Education in Early Childhood', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Early identification and intervention for developmental delays.', TRUE),
    ('ECED-HEALTH', 'Child Health, Safety, and Nutrition', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Pediatric first aid, balanced nutrition, child welfare laws.', TRUE),
    ('ECED-ENV', 'Preschool Classroom Environment & Design', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Learning centers, safety ergonomics, developmentally appropriate toys.', TRUE),
    ('ECED-FAM', 'Family and Community Partnerships in Early Childhood', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Parent engagement, home-school collaboration models.', TRUE),
    ('ECED-ASSESS', 'Observational Assessment and Child Portfolios', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Anecdotal records, developmental checklists, ECCD checklist.', TRUE),
    ('ECED-INFANT', 'Infant and Toddler Caregiving and Pedagogy', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Sensory stimulation, attachment theory, toddler routines.', TRUE),
    ('ECED-MUSIC', 'Music, Movement, and Dramatics for Young Children', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Rhythmic games, nursery rhymes, puppet theatre.', TRUE),
    ('ECED-RES', 'Research in Early Childhood Education', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Preschool pedagogical research and action study defense.', TRUE),
    ('SNED-INCL', 'Inclusive Practices for Exceptional Children', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Classroom modifications, peer tutoring, positive behavior support.', TRUE),
    ('SNED-BRAILLE', 'Braille and Orientation and Mobility', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Braille reading/writing, assistive technology for visual impairments.', TRUE),
    ('SNED-SIGN', 'Filipino Sign Language and Deaf Culture', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'FSL structure, receptive/expressive signing, Deaf education.', TRUE),
    ('SNED-AUTISM', 'Education of Learners with Autism Spectrum Disorder', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Sensory integration, TEACCH, visual schedules, communication.', TRUE),
    ('SNED-GIFTED', 'Curriculum Adaptation for Gifted Learners', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Enrichment strategies, acceleration, creative problem-solving.', TRUE),
    ('SNED-ASSESS', 'Psychoeducational Assessment in Special Education', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Diagnostic testing, IEP development, curriculum-based assessment.', TRUE),
    ('SNED-BEHAV', 'Applied Behavior Analysis in Special Education', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Functional behavior assessment, positive reinforcers, token economy.', TRUE),
    ('SNED-INTEL', 'Education of Learners with Intellectual Disabilities', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Adaptive life skills, functional academics, vocational transition.', TRUE),
    ('SNED-PHYS', 'Education of Learners with Physical & Health Impairments', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Assistive technologies, mobility adaptations, cerebral palsy.', TRUE),
    ('SNED-TRANS', 'Transition Programs for Learners with Special Needs', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Workplace readiness, supported employment, community living.', TRUE),
    ('SNED-CURR', 'Curriculum Adaptations and Accommodations in SPED', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Universal Design for Learning (UDL), individualized instruction.', TRUE),
    ('SNED-COMM', 'Counseling and Guidance for Families of SPED Learners', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Family counseling, crisis intervention, disability rights.', TRUE),
    ('SNED-RES', 'Research in Special Needs Education', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Empirical research in exceptional education and defense.', TRUE),
    ('PED-ANAT', 'Anatomy and Physiology of Human Movement', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Musculoskeletal, cardiovascular, and nervous system dynamics.', TRUE),
    ('PED-PHYS', 'Applied Kinesiology and Biomechanics in PE', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Lever systems, force vectors, motion analysis in athletic sports.', TRUE),
    ('PED-MOVE', 'Motor Learning and Development', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Stages of motor skill acquisition, neural control of movement.', TRUE),
    ('PED-GAMES', 'Teaching Individual and Dual Sports', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Badminton, table tennis, athletics pedagogical coaching.', TRUE),
    ('PED-DANCE', 'Philippine Traditional and Folk Dances', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Cultural dances, choreography, indigenous rhythms.', TRUE),
    ('PED-COACH', 'Sports Coaching and Officiating', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Tournament management, ethics in sports, rules of officiating.', TRUE),
    ('PED-TEAM1', 'Teaching Team Sports: Basketball and Volleyball', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Fundamental drills, game tactics, refereeing mechanics.', TRUE),
    ('PED-TEAM2', 'Teaching Team Sports: Soccer and Baseball/Softball', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Field positioning, defensive systems, skill progressions.', TRUE),
    ('PED-SWIM', 'Aquatics and Swimming Pedagogy', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Water safety, stroke mechanics (freestyle, breaststroke), lifesaving.', TRUE),
    ('PED-GYM', 'Gymnastics and Rhythmic Activities', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Floor exercises, balance beam, vault, apparatus safety.', TRUE),
    ('PED-FIT', 'Fitness Testing and Exercise Prescription', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Cardiovascular assessment, strength metrics, program design.', TRUE),
    ('PED-ADM', 'Organization and Administration of PE & Sports', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Intramural management, budget allocation, athletic facilities.', TRUE),
    ('PED-RES', 'Research in Physical Education and Sports', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Sports science and pedagogical physical education research.', TRUE),
    ('TLE-HE1', 'Culinary Arts and Food Preparation', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Baking, meal management, commercial cookery, sanitation.', TRUE),
    ('TLE-HE2', 'Clothing Construction and Design', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Pattern drafting, garment construction, sewing machine care.', TRUE),
    ('TLE-HE3', 'Home Management and Interior Design', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Family resource allocation, consumer education, residential space.', TRUE),
    ('TLE-HE4', 'Food Processing and Preservation Techniques', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Fermentation, curing, dehydration, canning, food chemistry.', TRUE),
    ('TLE-HE5', 'Commercial Baking and Pastry Production', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Artisan bread, pastry decoration, commercial bakery layout.', TRUE),
    ('TLE-HE6', 'Cosmetology, Hair and Beauty Care Pedagogy', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Skin care, hair cutting and styling, salon sanitation.', TRUE),
    ('TLE-HE7', 'Crafts and Cottage Industries Production', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Macrame, basketry, indigenous materials, market pitching.', TRUE),
    ('TLE-HE8', 'Household Services and Caregiving Management', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Elderly/child care, domestic management, hygiene protocols.', TRUE),
    ('TLE-HE9', 'Hospitality Operations & Front Office Pedagogy', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Guest reservation, concierge, hotel customer service.', TRUE),
    ('TLE-HE10', 'Fashion Accessories and Apparel Embellishment', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Embroidery, beading, costume jewelry design.', TRUE),
    ('TLE-HE11', 'Entrepreneurial Ventures in Home Economics', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Food kiosk management, boutique setup, cost analysis.', TRUE),
    ('TLE-HE12', 'Teaching Strategies in Technology & Livelihood Ed', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Competency-based training (CBT) modules, TESDA TR alignment.', TRUE),
    ('TLE-IA1', 'Civil and Architectural Woodworking', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Joinery, wood finishing, furniture construction, shop tools.', TRUE),
    ('TLE-IA2', 'Metalworking and Sheet Metal Processing', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Bench work, sheet metal fabrication, oxy-acetylene welding.', TRUE),
    ('TLE-IA3', 'Residential Electrical Installation and Safety', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Branch circuits, Philippine Electrical Code, safety wiring.', TRUE),
    ('TLE-IA4', 'Plumbing Installation and Sanitation Systems', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Piping materials, drainage systems, fixtures, water pressure.', TRUE),
    ('TLE-IA5', 'Refrigeration Fundamentals for Industrial Arts', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Domestic refrigerators, window AC repair, refrigerant recovery.', TRUE),
    ('TLE-IA6', 'Automotive Servicing and Engine Tune-up Fundamentals', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Lubrication, cooling systems, brake servicing, wheel alignment.', TRUE),
    ('TLE-IA7', 'Shielded Metal Arc Welding (SMAW NC II Level)', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Weld defects, fillet/groove welds, structural joint preparation.', TRUE),
    ('TLE-IA8', 'CADD and Architectural Drafting for Shop Teachers', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', '2D drafting, floor plans, elevations, section views.', TRUE),
    ('TLE-IA9', 'Masonry and Tile Setting Technologies', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Concrete hollow blocks, mortar mixing, floor tiling, estimating.', TRUE),
    ('TLE-IA10', 'Electronics Repair for Industrial Arts', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Power supply troubleshooting, soldering, audio amplifiers.', TRUE),
    ('TLE-IA11', 'Industrial Arts Shop Administration and OSH', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Tool inventory, preventive maintenance, DOLE OSH standards.', TRUE),
    ('TLE-IA12', 'Teaching Strategies in Industrial Arts Education', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Demonstration method, workshop rubrics, CBT instructional packages.', TRUE),
    ('TVED-ELEC1', 'Electrical Machine Fundamentals and Control', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Motors, transformers, relays, PLC control in tech-voc education.', TRUE),
    ('TVED-ELEC2', 'Industrial Wiring and Power Distribution', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Conduit bending, motor control circuits, switchgear maintenance.', TRUE),
    ('TVED-ELEC3', 'Renewable Energy Systems and Solar PV Technology', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Grid-tied/off-grid solar installation, inverter diagnostics.', TRUE),
    ('TVED-ELEC4', 'Programmable Logic Controllers in Electrical Automation', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'PLC ladder logic programming, HMI interfacing, sensors.', TRUE),
    ('TVED-ELEC5', 'Pneumatics and Electro-Pneumatics Control', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Directional control valves, pneumatic actuators, sequence logic.', TRUE),
    ('TVED-ELEC6', 'Building Management and Fire Alarm Systems', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Smoke detector loops, notification appliances, CCTV integration.', TRUE),
    ('TVED-ELEC7', 'Electric Motor Rewinding and Overhauling', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Stator winding insulation, coil span, varnish baking, testing.', TRUE),
    ('TVED-ELEC8', 'Instrumentation and Process Control for Educators', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Thermocouples, 4-20mA loops, PID controllers, calibration.', TRUE),
    ('TVED-ELEC9', 'Electrical Estimating and Costing for Tech-Voc', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Bill of materials, labor cost analysis, PEC branch circuit sizing.', TRUE),
    ('TVED-ELEC10', 'Competency Assessment in Electrical Installation NC II', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'TESDA assessment tools, evidence gathering, portfolio evaluation.', TRUE),
    ('TVED-ELEC11', 'Advanced Industrial Motor Control Systems', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Variable frequency drives (VFD), soft starters, braking methods.', TRUE),
    ('TVED-ELEC12', 'Curriculum Design for Technical-Vocational Education', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Competency-based curriculum packages, training regulations.', TRUE),
    ('TVED-ELX1', 'Solid-State Circuit Analysis and Troubleshooting', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Power supplies, amplifiers, test equipment in tech-voc.', TRUE),
    ('TVED-ELX2', 'Microcontroller Interfacing and Robotics Teaching', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Embedded programming, actuators, sensory feedback in schools.', TRUE),
    ('TVED-ELX3', 'Consumer Electronics Servicing and Repair', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Audio/video equipment diagnostics, PCB repair, component testing.', TRUE),
    ('TVED-ELX4', 'Digital Communications and Networking in Tech-Voc', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Serial protocols, Modbus, wireless sensor networks, IoT.', TRUE),
    ('TVED-ELX5', 'Optoelectronics and Fiber Optic Installation', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'LEDs, photodiodes, fiber splicing, OTDR testing.', TRUE),
    ('TVED-ELX6', 'SMD Soldering and PCB Fabrication Techniques', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Surface mount soldering, hot-air rework, etching, Gerber files.', TRUE),
    ('TVED-ELX7', 'Biomedical Equipment Servicing Fundamentals', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Patient monitors, ECG sensors, calibration, hospital safety.', TRUE),
    ('TVED-ELX8', 'Mechatronics Systems Integration Pedagogy', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Modular production systems, conveyor sorting, stepper motors.', TRUE),
    ('TVED-ELX9', 'Electronic Instrumentation and Measurement', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Digital oscilloscopes, spectrum analyzers, signal generators.', TRUE),
    ('TVED-ELX10', 'Competency Assessment in Electronics Products Assembly', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'TESDA EPAS NC II assessment procedures and institutional rubric.', TRUE),
    ('TVED-ELX11', 'Automotive Electronic Systems Troubleshooting', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'OBD-II scanner diagnosis, CAN bus troubleshooting, sensors.', TRUE),
    ('TVED-ELX12', 'Pedagogical Methods in Tech-Voc Electronics', 3.00, 0.00, 3.00, 3, 0, 'SPECIALIZATION_MAJOR', 'Job sheets, task analysis, institutional competency evaluations.', TRUE),
    ('BA-MAN', 'Principles of Management and Organization', 3.00, 0.00, 3.00, 3, 0, 'BUSINESS_CORE', 'Functions of management: planning, organizing, leading, controlling.', TRUE),
    ('BA-MKT', 'Principles of Marketing', 3.00, 0.00, 3.00, 3, 0, 'BUSINESS_CORE', 'Marketing mix (4Ps), market segmentation, consumer behaviour.', TRUE),
    ('BA-MICROE', 'Microeconomic Theory and Practice', 3.00, 0.00, 3.00, 3, 0, 'BUSINESS_CORE', 'Demand and supply, elasticity, consumer choice, market structures.', TRUE),
    ('BA-MACROE', 'Macroeconomic Theory and Practice', 3.00, 0.00, 3.00, 3, 0, 'BUSINESS_CORE', 'National income accounting, monetary/fiscal policy, inflation.', TRUE),
    ('BA-OBLI', 'Law on Obligations and Contracts', 3.00, 0.00, 3.00, 3, 0, 'BUSINESS_CORE', 'General principles of civil obligations, contracts, remedies.', TRUE),
    ('BA-BUSLAW', 'Business Laws and Regulatory Framework', 3.00, 0.00, 3.00, 3, 0, 'BUSINESS_CORE', 'Partnerships, corporations, negotiable instruments, securities.', TRUE),
    ('BA-TAX1', 'Income Taxation', 3.00, 0.00, 3.00, 3, 0, 'BUSINESS_CORE', 'Individual and corporate income taxes, capital gains, deductions.', TRUE),
    ('BA-TAX2', 'Business and Transfer Taxation', 3.00, 0.00, 3.00, 3, 0, 'BUSINESS_CORE', 'Value-added tax, percentage taxes, estate and donor taxes.', TRUE),
    ('BA-OPS', 'Operations Management and TQM', 3.00, 0.00, 3.00, 3, 0, 'BUSINESS_CORE', 'Production planning, inventory control, Six Sigma, supply chain.', TRUE),
    ('BA-STRAT', 'Strategic Management', 3.00, 0.00, 3.00, 3, 0, 'BUSINESS_CORE', 'Environmental scanning, corporate strategy formulation, implementation.', TRUE),
    ('ACT-FAR', 'Financial Accounting and Reporting', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Accounting cycle, financial statements preparation under PFRS.', TRUE),
    ('ACT-CFAS', 'Conceptual Framework and Accounting Standards', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'IASB Conceptual Framework, qualitative characteristics, recognition.', TRUE),
    ('ACT-IA1', 'Intermediate Accounting 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Accounting for cash, receivables, inventories, biological assets.', TRUE),
    ('ACT-IA2', 'Intermediate Accounting 2', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'PPE, intangible assets, liabilities, provisions, contingencies.', TRUE),
    ('ACT-IA3', 'Intermediate Accounting 3', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Shareholders equity, share-based payments, earnings per share.', TRUE),
    ('ACT-COST', 'Cost Accounting and Control', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Job order and process costing, standard costing, variance analysis.', TRUE),
    ('ACT-SCM', 'Strategic Cost Management', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Activity-based costing, target costing, balanced scorecard.', TRUE),
    ('ACT-AFAR1', 'Advanced Financial Accounting & Reporting 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Business combinations, consolidated financial statements.', TRUE),
    ('ACT-AFAR2', 'Advanced Financial Accounting & Reporting 2', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Foreign currency transactions, derivatives, government accounting.', TRUE),
    ('ACT-AUD1', 'Auditing and Assurance Principles', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'PSA standards, audit risk model, internal control evaluation.', TRUE),
    ('ACT-AUD2', 'Auditing and Assurance: Specialized Industries', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Substantive testing, financial sector auditing, audit reports.', TRUE),
    ('ACT-AUDCIS', 'Auditing in a CIS/IT Environment', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Computer-assisted audit techniques (CAATs), cybersecurity controls.', TRUE),
    ('ACT-INTEG1', 'Accounting Integration & Synthesis 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Comprehensive CPA board exam review: FAR, AFAR, Auditing.', TRUE),
    ('ACT-INTEG2', 'Accounting Integration & Synthesis 2', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Comprehensive CPA board exam review: Taxation, RFBT, MAS.', TRUE),
    ('ACT-INT', 'Accountancy Internship Practicum', 0.00, 6.00, 6.00, 0, 18, 'PRACTICUM', 'Supervised professional accounting internship in CPA firm.', TRUE),
    ('MA-COST1', 'Management Accounting Concepts & Applications', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'CVP analysis, relevant costing for decision-making, budgeting.', TRUE),
    ('MA-COST2', 'Quantitative Techniques in Management Accounting', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Linear programming, regression analysis, decision trees in business.', TRUE),
    ('MA-PERF', 'Financial Performance Evaluation & Control', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Responsibility accounting, transfer pricing, EVA, KPIs.', TRUE),
    ('MA-QUANT', 'Management Science and Operations Analytics', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Queuing theory, inventory optimization models, forecasting.', TRUE),
    ('MA-AUDIT', 'Internal Auditing and Operational Review', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'COSO framework, operational audit, risk management assessment.', TRUE),
    ('MA-FIN', 'Corporate Financial Reporting for Managers', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Financial statement analysis, cash flows, solvency ratios.', TRUE),
    ('MA-INT', 'Management Accounting Internship (600 Hours)', 0.00, 6.00, 6.00, 0, 18, 'PRACTICUM', 'Corporate management accounting and budgeting practical immersion.', TRUE),
    ('FM-FINMAN', 'Financial Management Principles', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Working capital management, capital budgeting, leverage, dividend.', TRUE),
    ('FM-MARKET', 'Financial Markets and Institutions', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Money and capital markets, central banking, securities trading.', TRUE),
    ('FM-INVEST', 'Investment and Portfolio Management', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Markowitz portfolio theory, CAPM, equity valuation, bonds.', TRUE),
    ('FM-CREDIT', 'Credit and Collection Management', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Credit policy evaluation, 5Cs of credit, accounts receivable.', TRUE),
    ('FM-PUBLIC', 'Public Finance and Fiscal Administration', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Government revenues, public debt, national budgeting processes.', TRUE),
    ('FM-GLOBAL', 'International Finance and Foreign Exchange Markets', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Currency hedging, forex derivatives, multinational financial management.', TRUE),
    ('FM-INT', 'Financial Management Practicum (600 Hours)', 0.00, 6.00, 6.00, 0, 18, 'PRACTICUM', 'Supervised banking, investment, or corporate finance immersion.', TRUE),
    ('ENT-OPP', 'Opportunity Seeking and Business Ideation', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Identifying market gaps, creative problem solving, feasibility.', TRUE),
    ('ENT-PLAN', 'Business Plan Preparation and Defense', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Comprehensive business plan formulation, financial projections.', TRUE),
    ('ENT-INNOV', 'Product Development and Innovation', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Prototyping, minimum viable product (MVP), design thinking.', TRUE),
    ('ENT-FIN', 'Entrepreneurial Finance and Venture Capital', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Seed funding, angel investing, term sheets, equity crowdfunding.', TRUE),
    ('ENT-GROWTH', 'Business Enterprise Implementation 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Enterprise launching, supply chain setup, daily operations.', TRUE),
    ('ENT-SCALE', 'Business Enterprise Implementation 2 (Scaling & Exit)', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Revenue scaling, franchising, partnership liquidation, defense.', TRUE),
    ('ENT-INT', 'Entrepreneurship Mentorship & Practicum', 0.00, 6.00, 6.00, 0, 18, 'PRACTICUM', 'Incubator/accelerator enterprise immersion, business showcase.', TRUE),
    ('HM-KITCHEN', 'Kitchen Essentials and Basic Food Preparation', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Culinary knife skills, food safety, HACCP, commercial kitchen equipment.', TRUE),
    ('HM-FB', 'Food and Beverage Service Operations', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Table setting, banqueting, beverage service, dining room management.', TRUE),
    ('HM-ROOMS', 'Rooms Division and Front Office Operations', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'PMS software, guest check-in, night audit, housekeeping procedures.', TRUE),
    ('HM-EVENT', 'Events and Convention Management (MICE)', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Meetings, incentives, conferences, exhibitions planning and bidding.', TRUE),
    ('HM-BAR', 'Bar and Beverage Management with Mixology', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Distillation, cocktails, wine appreciation, responsible alcohol service.', TRUE),
    ('HM-CULINARY', 'International Cuisine and Banquet Cookery', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'European, Asian, American cuisines, volume food production.', TRUE),
    ('HM-TOUR', 'Tourism and Hospitality Marketing Strategies', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Destination marketing, hospitality consumer experience, OTAs.', TRUE),
    ('HM-PRAC', 'Hospitality Industry Internship (600 Hours)', 0.00, 6.00, 6.00, 0, 18, 'PRACTICUM', 'Supervised luxury hotel, resort, or airline hospitality training.', TRUE),
    ('OA-KEY', 'Keyboarding and Document Processing', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Speed typing, correspondence formatting, word processing.', TRUE),
    ('OA-PROC', 'Advanced Office Procedures and Customer Service', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Office etiquette, travel arrangements, meeting scheduling, telephonics.', TRUE),
    ('OA-RECORDS', 'Records Management and Filing Systems', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Alphabetical, numerical, electronic record indexing, archival.', TRUE),
    ('OA-COMM', 'Business Report Writing and Corporate Communications', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Memoranda, executive summaries, press releases, corporate minutes.', TRUE),
    ('OA-SHORTHAND', 'Machine Shorthand and Transcription Technologies', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Stenotype theory, phonetic transcription, court reporting basics.', TRUE),
    ('OA-DESK', 'Desktop Publishing & Executive Office Presentation', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Graphic layout, brochures, digital presentations, spreadsheets.', TRUE),
    ('OA-EXEC', 'Executive Office Administration Internship (600 Hours)', 0.00, 6.00, 6.00, 0, 18, 'PRACTICUM', 'Supervised executive assistant internship in multinational or law firm.', TRUE),
    ('BIT-DRAW1', 'Technical Drawing and Blueprint Reading', 2.00, 1.00, 3.00, 2, 3, 'INDUSTRIAL_CORE', 'Orthographic projections, isometric drawing, dimensioning, tolerances.', TRUE),
    ('BIT-DRAW2', 'Computer-Aided Design and Drafting (CADD)', 1.00, 2.00, 3.00, 1, 6, 'INDUSTRIAL_CORE', '2D drafting, 3D solid modeling, rendering, parametric design.', TRUE),
    ('BIT-SHOP', 'Industrial Safety and Workshop Principles', 2.00, 0.00, 2.00, 2, 0, 'INDUSTRIAL_CORE', 'OSHA standards, hazard identification, fire safety, machine guarding.', TRUE),
    ('BIT-MGT', 'Industrial Organization and Shop Management', 3.00, 0.00, 3.00, 3, 0, 'INDUSTRIAL_CORE', 'Plant layout, inventory control, maintenance management, labor relations.', TRUE),
    ('BIT-RES1', 'Technology Research 1 (Proposal)', 2.00, 0.00, 2.00, 2, 0, 'SPECIALIZATION_MAJOR', 'Applied industrial technology problem identification and project proposal.', TRUE),
    ('BIT-RES2', 'Technology Research 2 (Fabrication & Defense)', 1.00, 2.00, 3.00, 1, 6, 'SPECIALIZATION_MAJOR', 'Prototype fabrication, operational testing, and technical panel defense.', TRUE),
    ('BIT-OJT1', 'Supervised Industrial Training 1 (In-Plant)', 0.00, 6.00, 6.00, 0, 18, 'PRACTICUM', 'Supervised shop floor training in accredited industrial partner.', TRUE),
    ('BIT-OJT2', 'Supervised Industrial Training 2 (Advanced In-Plant)', 0.00, 6.00, 6.00, 0, 18, 'PRACTICUM', 'Advanced manufacturing or industrial maintenance company placement.', TRUE),
    ('BIT-AUTO1', 'Automotive Power Trains and Underchassis', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Clutch, transmission, differentials, drive shafts, suspension.', TRUE),
    ('BIT-AUTO2', 'Automotive Engine Overhauling and Fuel Systems', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Cylinder block reboring, piston ring fitting, fuel injection, valve timing.', TRUE),
    ('BIT-AUTO3', 'Automotive Electrical and Electronic Systems', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Starting, charging, ignition circuits, ECU wiring, multiplexing.', TRUE),
    ('BIT-AUTO4', 'Advanced Engine Diagnostics and Tune-up', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Oscilloscope diagnostics, fuel trim, emissions analyzers, dyno testing.', TRUE),
    ('BIT-AUTO5', 'Heavy Equipment Operation and Maintenance', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Hydraulic pumps, heavy diesel engines, track systems.', TRUE),
    ('BIT-AUTO6', 'Electric Vehicle Fundamentals & Hybrid Systems', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'High-voltage battery safety, regenerative braking, inverter drives.', TRUE),
    ('BIT-COMP1', 'Computer Hardware and Maintenance Tech', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Motherboard diagnostics, CPU socket repair, power supplies.', TRUE),
    ('BIT-COMP2', 'Applied Computer Networks and Routing', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Ethernet cable crimping, patch panels, switch configurations.', TRUE),
    ('BIT-COMP3', 'Network Systems Administration for Technicians', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Windows Server, Active Directory, DNS, DHCP, RAID configuration.', TRUE),
    ('BIT-COMP4', 'Computer Diagnostics and Peripheral Repair', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Laser printer servicing, monitor power boards, BIOS reprogramming.', TRUE),
    ('BIT-COMP5', 'Cybersecurity Appliance Setup & Firewalls', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'PFSense, hardware UTM, IDS/IPS appliance configuration.', TRUE),
    ('BIT-COMP6', 'Data Center Operations & Server Rack Infrastructure', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Precision cooling, server rack cable management, UPS maintenance.', TRUE),
    ('BIT-ELEC1', 'Residential and Commercial Electrical Wiring', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'EMT/RMC conduit bending, branch circuit wiring, load centers.', TRUE),
    ('BIT-ELEC2', 'Industrial Motor Controls and Magnetic Starters', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Pushbutton stations, forward-reverse, wye-delta, interlocking.', TRUE),
    ('BIT-ELEC3', 'Electrical Machine Rewinding and Servicing', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Single-phase and three-phase motor stator rewinding, testing.', TRUE),
    ('BIT-ELEC4', 'Substation Maintenance and Switchgear Systems', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Circuit breakers, disconnect switches, lightning arresters, insulation.', TRUE),
    ('BIT-ELEC5', 'Renewable Energy Solar PV Microgrids', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Solar panel array mounting, charge controllers, net metering.', TRUE),
    ('BIT-ELEC6', 'Industrial Automation and PLC Wiring', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Relay logic migration, PLC I/O wiring, automation sensors.', TRUE),
    ('BIT-ELX1', 'Electronic Devices and Testing Equipment', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Passive/active component testing, analog/digital multimeters, CRO.', TRUE),
    ('BIT-ELX2', 'Audio and Video Equipment Servicing', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Power supplies, amplifiers, smart TV inverter boards, LCD repair.', TRUE),
    ('BIT-ELX3', 'Industrial Electronics and Sensor Systems', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Proximity sensors, photo-electric eyes, optocouplers, SCRs, TRIACs.', TRUE),
    ('BIT-ELX4', 'Microcontroller Systems and Robotics Interfacing', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Arduino/ESP32 programming, motor drivers, servo motors, sensors.', TRUE),
    ('BIT-ELX5', 'Electronic Product Assembly & Surface Mount Technology', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'SMD component rework, pick-and-place soldering, flux cleaning.', TRUE),
    ('BIT-ELX6', 'CCTV, Security and Access Control Installation', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'IP camera configuration, NVR setup, RFID card reader integration.', TRUE),
    ('BIT-MECH1', 'Benchwork and Machine Shop Practice (Lathe & Shaper)', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Center lathe operations, turning, knurling, thread cutting, shaping.', TRUE),
    ('BIT-MECH2', 'Milling Machine Operations and Gear Cutting', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Horizontal/vertical milling, indexing heads, spur/helical gears.', TRUE),
    ('BIT-MECH3', 'Shielded Metal Arc Welding (SMAW 1G to 4G)', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Groove welds, root pass, hot pass, capping in flat and overhead.', TRUE),
    ('BIT-MECH4', 'Gas Metal Arc and Gas Tungsten Arc Welding (GMAW/GTAW)', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'MIG/TIG welding of carbon steel, stainless steel, and aluminum.', TRUE),
    ('BIT-MECH5', 'CNC Machine Programming and Tooling', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'G-codes, M-codes, CNC lathe/milling tool offset calibration.', TRUE),
    ('BIT-MECH6', 'Industrial Hydraulics and Pneumatics Mechanics', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Hydraulic cylinders, control manifolds, accumulator servicing.', TRUE),
    ('BIT-DRAFT1', 'Architectural Drafting and Working Drawings', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Site development plans, architectural floor plans, elevations, sections.', TRUE),
    ('BIT-DRAFT2', 'Structural and Civil CADD Drafting', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Foundation plans, column schedules, beam details, slab rebar drawings.', TRUE),
    ('BIT-DRAFT3', 'Mechanical, Electrical & Plumbing (MEP) Drafting', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Electrical layout, lighting circuits, plumbing isometrics, HVAC ducts.', TRUE),
    ('BIT-DRAFT4', 'Building Information Modeling (BIM - Revit)', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Revit 3D models, families, clash detection, quantity take-offs.', TRUE),
    ('BIT-DRAFT5', '3D Architectural Rendering and Visualization', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Lumion/V-Ray materials, realistic lighting, walkthrough animations.', TRUE),
    ('BIT-DRAFT6', 'Geographic Information Systems (GIS) Mapping', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'QGIS layers, topological surveys, thematic land use mapping.', TRUE),
    ('BIT-HVAC1', 'Domestic Refrigeration and Air Conditioning Servicing', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Compressors, condensers, capillary tubes, charging, brazing.', TRUE),
    ('BIT-HVAC2', 'Commercial Refrigeration Systems and Cold Storage', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Thermostatic expansion valves, walk-in freezers, defrost timers.', TRUE),
    ('BIT-HVAC3', 'Central Air Conditioning and Chilled Water Systems', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Air handling units (AHUs), cooling towers, water chillers, balancing.', TRUE),
    ('BIT-HVAC4', 'HVAC Electrical Controls and Troubleshooting', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Capacitors, contactors, overload protectors, digital thermostats.', TRUE),
    ('BIT-HVAC5', 'Ventilation System Design and Ductwork Fabrication', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Duct sizing, sheet metal transitions, dampers, acoustic insulation.', TRUE),
    ('BIT-HVAC6', 'Automotive Air Conditioning Maintenance', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Compressor clutches, expansion valves, leak detection, R-134a/R-1234yf.', TRUE),
    ('BIT-CUL1', 'Commercial Food Preparation and Kitchen Equipment', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Stocks, sauces, soups, meat cutting, commercial range maintenance.', TRUE),
    ('BIT-CUL2', 'Baking and Pastry Arts Technology', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Yeast doughs, laminated pastries, artisan breads, cake decorating.', TRUE),
    ('BIT-CUL3', 'International Cookery and Asian Cuisines', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Wok cookery, sushi/sashimi, curry profiles, regional Asian dishes.', TRUE),
    ('BIT-CUL4', 'Quantity Food Production and Institutional Catering', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Menu engineering, banquet logistics, food costing, hot holding.', TRUE),
    ('BIT-CUL5', 'Garde Manger and Charcuterie Production', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Terrines, pates, cured sausages, canapes, salad dressings.', TRUE),
    ('BIT-CUL6', 'Barista Skills, Beverage Service and Bartending', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Espresso extraction, latte art, mocktails, inventory control.', TRUE),
    ('BIT-FASH1', 'Pattern Drafting and Garment Construction', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Body measurements, sloper drafting, seam allowances, darts, zippers.', TRUE),
    ('BIT-FASH2', 'Children and Women Casual Wear Production', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Dresses, blouses, skirts, children rompers, buttonholing.', TRUE),
    ('BIT-FASH3', 'Tailoring and Menswear Construction', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Trousers, polo shirts, suits, interfacing, collar stays, pressing.', TRUE),
    ('BIT-FASH4', 'Haute Couture, Evening Gowns and Costume Design', 2.00, 2.00, 4.00, 2, 6, 'SPECIALIZATION_MAJOR', 'Boning, corsetry, beadwork, draping on dress forms.', TRUE),
    ('BIT-FASH5', 'Textile Science, Fabric Identification and Dyeing', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Natural and synthetic fibers, weave patterns, batik, fabric testing.', TRUE),
    ('BIT-FASH6', 'Fashion Illustration and Digital Pattern Making', 2.00, 1.00, 3.00, 2, 3, 'SPECIALIZATION_MAJOR', 'Fashion croquis drawing, Gerber AccuMark, CAD grading.', TRUE),
    ('CRIM-INTRO', 'Introduction to Criminology', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Theories of crime causation, classical and positivist schools.', TRUE),
    ('CRIM-LEA1', 'Police Organization and Administration', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'PNP law (RA 6975/8551), administrative leadership, patrol ops.', TRUE),
    ('CRIM-LEA2', 'Comparative Models in Policing', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'INTERPOL, ASEANAPOL, policing models across Asia, US, and Europe.', TRUE),
    ('CRIM-PHOTO', 'Forensic Photography with Laboratory', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Crime scene photography, camera mechanics, digital imaging.', TRUE),
    ('CRIM-FINGER', 'Personal Identification Techniques (Dactyloscopy)', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Fingerprint classification, Henry system, latent prints, AFIS.', TRUE),
    ('CRIM-BALL', 'Forensic Ballistics with Laboratory', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Firearms identification, rifling marks, bullet comparison microscope.', TRUE),
    ('CRIM-POLY', 'Lie Detection and Polygraphy', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Polygraph instrument components, chart interpretation, pre-test interview.', TRUE),
    ('CRIM-DOC', 'Questioned Documents Examination', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Handwriting analysis, forgery detection, paper/ink forensic analysis.', TRUE),
    ('CRIM-LAW1', 'Criminal Law (Book 1 - RPC)', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'General principles of criminal liability, justifying circumstances.', TRUE),
    ('CRIM-LAW2', 'Criminal Law (Book 2 - Special Penal Laws)', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Crimes against persons, property, security; anti-cybercrime, drugs.', TRUE),
    ('CRIM-PROC', 'Criminal Procedure and Court Testimony', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Rules of Court Rule 110-127, arrest, search, bail, expert witness.', TRUE),
    ('CRIM-CORR1', 'Institutional Corrections', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Bureau of Corrections (BuCor), BJMP, prison management, rehabilitation.', TRUE),
    ('CRIM-CORR2', 'Non-Institutional Corrections', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Probation law (PD 968), parole, pardon, executive clemency.', TRUE),
    ('CRIM-DEF', 'Defensive Tactics and Martial Arts', 2.00, 0.00, 2.00, 2, 0, 'PROFESSIONAL_MAJOR', 'Arnis, judo/jiujitsu holds, handcuffing techniques, disarming.', TRUE),
    ('CRIM-MARK', 'Marksmanship and Combat Shooting', 1.00, 1.00, 2.00, 1, 3, 'PROFESSIONAL_MAJOR', 'Firearm safety, shooting positions, combat reloads, range qualification.', TRUE),
    ('CRIM-INV1', 'Specialized Crime Investigation 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Homicide, murder, robbery, physical injuries crime scene processing.', TRUE),
    ('CRIM-INV2', 'Specialized Crime Investigation 2', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Crimes against women and children, trafficking in persons, cyber crimes.', TRUE),
    ('CRIM-TRAF', 'Traffic Management and Accident Investigation', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Traffic laws, skid mark calculations, collision reconstruction.', TRUE),
    ('CRIM-DRUG', 'Drug Education and Vice Control', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'RA 9165 (Comprehensive Dangerous Drugs Act), field test kits.', TRUE),
    ('CRIM-THES1', 'Criminological Research and Statistics 1', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Research proposal on crime prevention and correctional systems.', TRUE),
    ('CRIM-THES2', 'Criminological Research and Statistics 2', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Data collection, crime mapping, quantitative analysis, defense.', TRUE),
    ('CRIM-PRAC1', 'Criminology Practicum 1 (Community Immersion)', 0.00, 3.00, 3.00, 0, 9, 'PRACTICUM', 'Community immersion, barangay justice system (KP), crime prevention.', TRUE),
    ('CRIM-PRAC2', 'Criminology Practicum 2 (Police Deployment)', 0.00, 3.00, 3.00, 0, 9, 'PRACTICUM', 'Police station deployment, patrol, blotter recording, traffic duties.', TRUE),
    ('PSY-INTRO', 'Introduction to Psychology', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Biological, cognitive, and social foundations of human behaviour.', TRUE),
    ('PSY-DEV', 'Developmental Psychology', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Prenatal, infancy, childhood, adolescence, adult life transitions.', TRUE),
    ('PSY-STAT', 'Psychological Statistics', 3.00, 1.00, 4.00, 3, 3, 'PROFESSIONAL_MAJOR', 'Parametric/non-parametric tests, ANOVA, regression, SPSS.', TRUE),
    ('PSY-PERS', 'Theories of Personality', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Psychoanalytic, trait, humanistic, behavioral, social-cognitive views.', TRUE),
    ('PSY-EXP', 'Experimental Psychology with Laboratory', 3.00, 2.00, 5.00, 3, 6, 'PROFESSIONAL_MAJOR', 'Scientific design, animal/human cognition trials, APA reports.', TRUE),
    ('PSY-ABN', 'Abnormal Psychology', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'DSM-5 diagnostic criteria, mood, anxiety, psychotic disorders.', TRUE),
    ('PSY-IO', 'Industrial / Organizational Psychology', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Job analysis, personnel selection, workplace motivation, leadership.', TRUE),
    ('PSY-BIO', 'Biopsychology and Neuroscience', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Neurons, neurotransmitters, brain structures, endocrine system.', TRUE),
    ('PSY-COG', 'Cognitive Psychology', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Attention, memory models, language processing, decision making.', TRUE),
    ('PSY-SOCIAL', 'Social Psychology', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Social cognition, conformity, prejudice, aggression, altruism.', TRUE),
    ('PSY-ASSESS', 'Psychological Assessment with Laboratory', 3.00, 2.00, 5.00, 3, 6, 'PROFESSIONAL_MAJOR', 'Administration and scoring of intelligence and personality test batteries.', TRUE),
    ('PSY-COUNSEL', 'Principles and Techniques of Counseling', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Therapeutic alliance, listening skills, ethical counseling standards.', TRUE),
    ('PSY-THES1', 'Research in Psychology 1 (Proposal Writing)', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Formulation of research question, literature review, ethics review.', TRUE),
    ('PSY-THES2', 'Research in Psychology 2 (Thesis Defense)', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Empirical data collection, statistical treatment, oral thesis defense.', TRUE),
    ('PSY-PRAC', 'Psychology Practicum (300 Hours)', 0.00, 5.00, 5.00, 0, 15, 'PRACTICUM', 'Supervised clinical, industrial, or educational psychological practicum.', TRUE),
    ('EL-INTRO', 'Introduction to the English Language Studies', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Origins, history, and development of the English language.', TRUE),
    ('EL-LING1', 'English Phonetics and Phonology', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Speech articulation, acoustics, phonemic transcription.', TRUE),
    ('EL-LING2', 'English Morphology and Syntax', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Word-formation processes, phrase structure, transformational grammar.', TRUE),
    ('EL-SEM', 'Semantics and Pragmatics of English', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Linguistic meaning, truth conditions, speech acts, conversational implicature.', TRUE),
    ('EL-PRAG', 'Discourse Analysis and Textual Pragmatics', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Cohesion, coherence, critical discourse analysis of media.', TRUE),
    ('EL-DISC', 'Sociolinguistics and World Englishes', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Language variation, Philippine English, language policy.', TRUE),
    ('EL-CORPUS', 'Corpus Linguistics and Computational Text Analysis', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Corpora concordancing, collocations, digital humanities text mining.', TRUE),
    ('EL-PSYCH', 'Psycholinguistics and Language Acquisition', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'First and second language acquisition, neural language processing.', TRUE),
    ('EL-COMM', 'English for Specific Purposes (ESP) & Corporate Writing', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Corporate communication, technical writing, executive editing.', TRUE),
    ('EL-INTER', 'Intercultural Communication & Translation Studies', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Cross-cultural pragmatics, localization, translation theory.', TRUE),
    ('EL-THES1', 'Language Research 1 (Proposal)', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Formulation of linguistic research design and theoretical framework.', TRUE),
    ('EL-THES2', 'Language Research 2 (Thesis Defense)', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Data analysis, findings presentation, and final oral defense.', TRUE),
    ('EL-PRAC', 'English Language Practicum (300 Hours)', 0.00, 5.00, 5.00, 0, 15, 'PRACTICUM', 'Corporate communications, editing, publishing, content writing immersion.', TRUE),
    ('SS-SOCIOL', 'Foundations of Sociology', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Social structures, institutions, social stratification, inequality.', TRUE),
    ('SS-ANTHRO', 'General Anthropology and Cultural Systems', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Physical and cultural anthropology, human diversity, ethnography.', TRUE),
    ('SS-POLSCI', 'Philippine Politics and Governance', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', '1987 Philippine Constitution, branches of government, public policy.', TRUE),
    ('SS-GEOG', 'Human and Environmental Geography', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Spatial patterns, population migration, urbanization, geography.', TRUE),
    ('SS-HIST', 'World History and Civilizations', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Ancient civilizations, Middle Ages, Industrial Revolution, modern era.', TRUE),
    ('SS-ASIAN', 'Asian Studies and Regional Dynamics', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Geopolitics, economic cooperation (ASEAN), cultural transformations.', TRUE),
    ('SS-DEV', 'Sociology of Development and Global Inequalities', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Modernization, dependency theory, SDGs, rural-urban disparities.', TRUE),
    ('SS-COMM', 'Community Organizing and Social Development', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Participatory action research, grass-roots empowerment, NGOs.', TRUE),
    ('SS-RES', 'Social Science Research Methods', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Quantitative and qualitative social science research paradigms.', TRUE),
    ('SS-STAT', 'Applied Statistics for Social Sciences', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Survey sampling, contingency tables, hypothesis testing.', TRUE),
    ('SS-THES1', 'Social Science Research 1', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Research proposal preparation in socio-political dynamics.', TRUE),
    ('SS-THES2', 'Social Science Research 2', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Field data gathering, thematic analysis, final defense.', TRUE),
    ('SS-PRAC', 'Social Science Community Practicum (300 Hours)', 0.00, 5.00, 5.00, 0, 15, 'PRACTICUM', 'Immersion with NGOs, LGUs, and civic community organizations.', TRUE),
    ('PA-THEORY', 'Theory and Practice of Public Administration', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Evolution of PA, New Public Management, governance models.', TRUE),
    ('PA-ADMIN', 'Philippine Administrative System', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Structure and operations of the executive branch, civil service.', TRUE),
    ('PA-HRM', 'Public Personnel Administration', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Civil service rules, merit system, public sector compensation.', TRUE),
    ('PA-FISCAL', 'Public Fiscal Administration', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Government budgeting, revenue generation, audit, COA circulars.', TRUE),
    ('PA-POLICY', 'Public Policy Analysis', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Policy cycle: formulation, adoption, implementation, evaluation.', TRUE),
    ('PA-LOCAL', 'Local Government and Regional Administration', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Local Government Code (RA 7160), decentralization, devolution.', TRUE),
    ('PA-ETHICS', 'Ethics and Accountability in the Public Service', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'RA 6713, anti-graft laws, Ombudsman functions, transparent governance.', TRUE),
    ('PA-ORG', 'Organizational Behavior in Public Agencies', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Bureaucracy, leadership styles, administrative culture, reform.', TRUE),
    ('PA-PLAN', 'Development Planning and Project Management in Government', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'NEDA planning process, project feasibility, LGU development plans.', TRUE),
    ('PA-LAW', 'Administrative Law and Judicial Review', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Quasi-legislative powers, due process, writs of mandamus and certiorari.', TRUE),
    ('PA-THES1', 'Research in Public Administration 1', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Governance research proposal design and policy formulation.', TRUE),
    ('PA-THES2', 'Research in Public Administration 2', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Policy impact empirical evaluation, thesis colloquium.', TRUE),
    ('PA-PRAC', 'Public Administration Internship (300 Hours)', 0.00, 5.00, 5.00, 0, 15, 'PRACTICUM', 'Government agency or local legislative office practicum.', TRUE),
    ('AM-CALC1', 'Advanced Mathematical Analysis 1', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Real analysis, sequences, metric spaces, topology of the real line.', TRUE),
    ('AM-CALC2', 'Advanced Mathematical Analysis 2', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Riemann-Stieltjes integration, uniform convergence, power series.', TRUE),
    ('AM-LINALG', 'Linear Algebra and Matrix Theory', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Vector spaces, linear transformations, eigenvalues, diagonalization.', TRUE),
    ('AM-DIFFEQ', 'Applied Differential Equations and Modeling', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Dynamical systems, mathematical modeling in biology and economics.', TRUE),
    ('AM-NUM', 'Numerical Analysis and Computational Methods', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Root finding, interpolation, numerical integration, MATLAB.', TRUE),
    ('AM-PROB', 'Probability Theory and Mathematical Statistics', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Random variables, probability distributions, central limit theorem.', TRUE),
    ('AM-OPTIM', 'Operations Research and Linear Programming', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Simplex algorithm, duality theory, sensitivity, integer programming.', TRUE),
    ('AM-COMPLEX', 'Complex Analysis and Conformal Mapping', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Analytic functions, Cauchy-Riemann, contour integrals, residues.', TRUE),
    ('AM-FINMATH', 'Mathematics of Financial Derivatives & Actuarial Sci', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Black-Scholes model, life contingencies, interest rate theory.', TRUE),
    ('AM-DATA', 'Statistical Computing and Data Science with R/Python', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Data wrangling, regression models, multivariate statistics in R.', TRUE),
    ('AM-THES1', 'Applied Mathematics Research 1', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Mathematical formulation and numerical simulation proposal.', TRUE),
    ('AM-THES2', 'Applied Mathematics Research 2', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Simulation validation, sensitivity analysis, defense.', TRUE),
    ('AM-PRAC', 'Industrial Mathematics Practicum (300 Hours)', 0.00, 5.00, 5.00, 0, 15, 'PRACTICUM', 'Data analytics, actuarial, or financial modeling industry placement.', TRUE),
    ('FISH-INTRO', 'Introduction to Fisheries and Aquatic Sciences', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Aquatic biology, marine biodiversity, sustainable fisheries.', TRUE),
    ('FISH-AQUA1', 'Freshwater Aquaculture with Fieldwork', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Tilapia/catfish culture, hatchery management, pond construction.', TRUE),
    ('FISH-AQUA2', 'Brackishwater and Marine Aquaculture', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Milkfish/shrimp farming, seaweeds, cage culture systems.', TRUE),
    ('FISH-CAPTURE', 'Capture Fisheries and Fishing Technology', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Fishing gears, boat navigation, acoustic detection systems.', TRUE),
    ('FISH-POST', 'Post-Harvest Fisheries and Fish Processing', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Smoking, drying, canning, HACCP standards in seafood handling.', TRUE),
    ('FISH-OCEAN', 'Biological Oceanography and Limnology', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Plankton dynamics, marine ecology, water quality parameters.', TRUE),
    ('FISH-ECOL', 'Aquatic Ecology and Resource Management', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Coral reef preservation, mangrove conservation, Fisheries Code.', TRUE),
    ('FISH-HEALTH', 'Fish Diseases, Pathology and Biosecurity', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Bacterial, fungal, parasitic fish infections, diagnosis, therapy.', TRUE),
    ('FISH-NUTR', 'Fish Nutrition, Feed Formulation & Manufacture', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Nutritional requirements, pelleting technology, FCR metrics.', TRUE),
    ('FISH-HATCH', 'Fish Hatchery Operations and Seed Production', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Induced spawning, live food culture (rotifers/artemia), nursing.', TRUE),
    ('FISH-LAWS', 'Fishery Laws, Policies and Coastal Zone Governance', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'RA 8550/10654, municipal fisheries, marine protected areas (MPAs).', TRUE),
    ('FISH-THES1', 'Fisheries Research 1 (Proposal)', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Aquaculture or marine fisheries scientific research proposal.', TRUE),
    ('FISH-THES2', 'Fisheries Research 2 (Defense)', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Laboratory experiment analysis, scientific paper colloquium.', TRUE),
    ('FISH-PRAC', 'Fisheries Industry On-the-Job Training', 0.00, 5.00, 5.00, 0, 15, 'PRACTICUM', 'Supervised training in commercial fish hatcheries or processing plants.', TRUE),
    ('CS-DISC', 'Discrete Structures for Computing', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Mathematical logic, proofs, sets, relations, graph theory, combinatorics.', TRUE),
    ('CS-ALGO', 'Design and Analysis of Algorithms', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Algorithm design paradigms, divide and conquer, dynamic programming, NP-completeness.', TRUE),
    ('CS-ARCH', 'Computer Architecture and Organization', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Instruction set architecture, CPU datapath, pipelining, memory hierarchy.', TRUE),
    ('CS-AUTO', 'Automata Theory and Formal Languages', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Finite automata, regular expressions, context-free grammars, Turing machines.', TRUE),
    ('CS-PROGLANG', 'Principles of Programming Languages', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Language paradigms, syntax, semantic analysis, type systems, runtime storage.', TRUE),
    ('CS-OS', 'Operating Systems Principles and Architecture', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Process management, concurrency, deadlock, virtual memory, file systems.', TRUE),
    ('CS-SEC', 'Information Assurance and Security for CS', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Applied cryptography, secure coding, access control models, system security.', TRUE),
    ('CS-SE1', 'Software Engineering 1 (Analysis & Design)', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Agile methodologies, software requirements specification, architectural design, UML.', TRUE),
    ('CS-SE2', 'Software Engineering 2 (Implementation & QA)', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Software testing, CI/CD, verification, automated testing frameworks, metrics.', TRUE),
    ('CS-AI', 'Artificial Intelligence and Intelligent Systems', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Knowledge representation, search algorithms, heuristic evaluation, machine learning.', TRUE),
    ('CS-DATA', 'Data Mining and Knowledge Discovery', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Data preprocessing, association rules, classification, clustering, big data pipelines.', TRUE),
    ('CS-GRAPH', 'Computer Graphics and Visual Computing', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', '2D/3D transformations, rendering pipelines, shaders, OpenGL/WebGL rendering.', TRUE),
    ('CS-PAR', 'Parallel and Distributed Computing Systems', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Multi-core programming, OpenMP, MPI, GPU acceleration, distributed consensus.', TRUE),
    ('CS-COMP', 'Compiler Design and Construction', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Lexical analysis, parsing, intermediate code generation, optimization, code gen.', TRUE),
    ('CS-MODEL', 'Computational Modeling and Simulation', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Discrete-event simulation, stochastic modeling, Monte Carlo methods.', TRUE),
    ('CS-THES1', 'CS Thesis 1 (Proposal & Defense)', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Scientific computing research problem formulation, literature review, proposal.', TRUE),
    ('CS-THES2', 'CS Thesis 2 (Implementation & Colloquium)', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Empirical evaluation, thesis paper publication defense, public presentation.', TRUE),
    ('CS-PRAC', 'CS Industry Internship (486 Hours)', 0.00, 6.00, 6.00, 0, 18, 'PRACTICUM', 'Supervised industry on-the-job training in software engineering or R&D.', TRUE),
    ('MM-BEHAV', 'Consumer Behavior', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Psychological, sociological, and cultural factors influencing consumer choices.', TRUE),
    ('MM-RES', 'Marketing Research', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Qualitative and quantitative market research, survey design, data interpretation.', TRUE),
    ('MM-COMM', 'Integrated Marketing Communications', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Advertising, public relations, sales promotion, and direct marketing integration.', TRUE),
    ('MM-PRIC', 'Pricing Strategy and Management', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Cost-volume-profit analysis, competitive pricing models, psychological pricing.', TRUE),
    ('MM-DIST', 'Distribution Management and Supply Chain', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Wholesaling, retailing, logistics, channel conflict, channel design.', TRUE),
    ('MM-DIG', 'Digital Marketing and E-Commerce Strategies', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'SEO, SEM, social media analytics, content marketing, e-commerce platforms.', TRUE),
    ('MM-STRAT', 'Strategic Marketing Management', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Competitive positioning, market penetration, growth strategies, brand portfolio.', TRUE),
    ('MM-SALES', 'Professional Salesmanship and Negotiation', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Personal selling process, customer relationship management, consultative selling.', TRUE),
    ('MM-SERV', 'Services Marketing', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Service quality gaps model, customer retention, service recovery strategies.', TRUE),
    ('MM-BRAND', 'Strategic Brand Management', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Brand equity building, brand architecture, co-branding, brand revitalizing.', TRUE),
    ('MM-INT', 'International Marketing', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Global market entry modes, cultural nuances, trade barriers, global branding.', TRUE),
    ('MM-PLAN', 'Marketing Plan Capstone Project', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Comprehensive marketing plan formulation and defense for commercial enterprise.', TRUE),
    ('HR-ADMIN', 'Human Resource Management Foundations', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Role of HR in strategic management, HR planning, employee lifecycle.', TRUE),
    ('HR-RECRUIT', 'Recruitment, Selection and Talent Acquisition', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Job analysis, sourcing channels, competency-based interviewing, selection tools.', TRUE),
    ('HR-TRAIN', 'Training and Development', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Training needs analysis, instructional design, Kirkpatrick evaluation framework.', TRUE),
    ('HR-COMP', 'Compensation and Wage Administration', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Job evaluation, salary structure design, incentive plans, statutory benefits.', TRUE),
    ('HR-REL', 'Labor Relations and Collective Bargaining', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Philippine Labor Code, union organization, CBA negotiation, grievance handling.', TRUE),
    ('HR-PERF', 'Performance Management Systems', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'KPIs, OKRs, 360-degree feedback, appraisal methods, coaching and mentoring.', TRUE),
    ('HR-ORG', 'Organizational Development and Change', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Organizational diagnosis, culture transformation, change management models.', TRUE),
    ('HR-ANALYT', 'HR Metrics and Workforce Analytics', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Attrition modeling, employee engagement metrics, HR dashboard analytics.', TRUE),
    ('HR-HEALTH', 'Occupational Safety, Health and Wellness', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'DOLE OSH standards, workplace ergonomics, mental health programs.', TRUE),
    ('HR-LAW', 'Labor Laws, Legislation and Statutory Benefits', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Philippine labor code, minimum wage, DOLE compliance, NLRC cases.', TRUE),
    ('HR-GLOBAL', 'Global Human Resource Management', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Expatriate management, cross-cultural leadership, international labor standards.', TRUE),
    ('HR-ETHICS', 'Good Governance and Social Responsibility in HRM', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Corporate governance, whistleblower policies, stakeholder ethics, CSR.', TRUE),
    ('HR-PLAN', 'Strategic HR Planning Capstone', 3.00, 0.00, 3.00, 3, 0, 'CAPSTONE', 'Comprehensive strategic workforce plan formulation and defense.', TRUE),
    ('SS-TRENDS', 'Trends and Critical Issues in Social Studies', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Globalization, climate change, geopolitical conflicts in teaching perspective.', TRUE),
    ('SS-TEACH', 'Teaching Approaches in Secondary Social Studies', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Inquiry-based learning, historical empathy, simulations, mock congresses.', TRUE),
    ('SS-MAT', 'Production of Social Studies Instructional Materials', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Primary source curation, digital mapping tools, multimedia instructional kits.', TRUE),
    ('ICT-HARD', 'Computer Hardware Servicing and Diagnostics', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'PC assembly, OS installation, hardware troubleshooting, preventative maintenance.', TRUE),
    ('ICT-NET', 'Computer Systems Networking & Cable Splicing', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'UTP termination, structured cabling, SOHO router setup, packet tracing.', TRUE),
    ('ICT-PROG', 'Programming Essentials for Tech-Voc Education', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Python scripting, logic formulation, educational app development.', TRUE),
    ('ICT-WEB', 'Web Development and Content Management Systems', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'WordPress/Joomla theming, responsive HTML5/CSS3, website deployment.', TRUE),
    ('ICT-MEDIA', 'Multimedia Authoring and Digital Media Production', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Audio-video editing, vector graphic design, educational animation.', TRUE),
    ('ICT-PEDAG', 'Specialized Pedagogical Methods in Teaching ICT', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Competency-based training (CBT) methodology, TESDA TR alignment.', TRUE),
    ('FSM-SAN', 'Food Safety, Sanitation and Hygiene Standards', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'HACCP principles, cross-contamination prevention, food handler certification.', TRUE),
    ('FSM-BAKE', 'Commercial Baking and Pastry Arts', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Yeast breads, laminated pastries, cake decoration, commercial bakery layout.', TRUE),
    ('FSM-COOK1', 'Commercial Food Preparation 1 (Western Cuisine)', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Stocks, mother sauces, butchery, banquet food prep techniques.', TRUE),
    ('FSM-COOK2', 'Commercial Food Preparation 2 (Asian & Heritage)', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Regional Filipino culinary heritage, Asian stir-fry, dim sum, curries.', TRUE),
    ('FSM-BAR', 'Beverage Management and Bar Operations', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Mixology, coffee cupping, wine appreciation, bar inventory control.', TRUE),
    ('FSM-CATER', 'Catering and Banquet Service Management', 2.00, 1.00, 3.00, 2, 3, 'PROFESSIONAL_MAJOR', 'Event contracts, buffet setup, service etiquette, banquet logistics.', TRUE),
    ('FSM-COST', 'Food Cost Control, Purchasing and Storeroom Mgmt', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'Recipe costing, yield management, requisition protocols, inventory auditing.', TRUE),
    ('FSM-PEDAG', 'Competency-Based Methodology in Food Technology', 3.00, 0.00, 3.00, 3, 0, 'PROFESSIONAL_MAJOR', 'TESDA cookery NC II assessment tools, workplace learning session planning.', TRUE)
AS new_c ON DUPLICATE KEY UPDATE title = new_c.title, credit_units = new_c.credit_units, category = new_c.category;

-- ============================================================================
-- 4. CURRICULA RECORDS SEEDING (ALL 47 ACTIVE CURRICULA)
-- ============================================================================

-- Clean up any legacy incomplete test curricula if they exist
DELETE FROM curriculum_courses WHERE curriculum_id IN (SELECT id FROM curricula WHERE code IN ('BSIT-2023-V1', 'BSCpE-2023-V1'));
DELETE FROM curricula WHERE code IN ('BSIT-2023-V1', 'BSCpE-2023-V1');

INSERT INTO curricula (program_id, major_id, code, name, status, version_number, effective_academic_year, is_active)
VALUES
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BA-EL' LIMIT 1), NULL, 'CURR-BAEL-2026', 'BA in English Language CMO 48 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BA-SS' LIMIT 1), NULL, 'CURR-BASS-2026', 'BA in Social Science CHED PSG Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BPA' LIMIT 1), NULL, 'CURR-BPA-2026', 'Bachelor of Public Administration CMO 06 s. 2010 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BS-AM' LIMIT 1), NULL, 'CURR-BSAM-2026', 'BS in Applied Mathematics CMO 19 s. 2022 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BS-PSYCH' LIMIT 1), NULL, 'CURR-PSYCH-2026', 'BS in Psychology CMO 34 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSCS' LIMIT 1), NULL, 'CURR-BSCS-2026', 'BS in Computer Science CMO 25 s. 2015 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSIT' LIMIT 1), NULL, 'CURR-BSIT-2026', 'BS in Information Technology CMO 25 s. 2015 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSIS' LIMIT 1), NULL, 'CURR-BSIS-2026', 'BS in Information Systems CMO 25 s. 2015 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSEd' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'MATH' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSEd' LIMIT 1) LIMIT 1), 'CURR-BSED-MATH-2026', 'BSEd Major in Mathematics CMO 75 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSEd' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'ENG' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSEd' LIMIT 1) LIMIT 1), 'CURR-BSED-ENG-2026', 'BSEd Major in English CMO 75 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSEd' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'SCI' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSEd' LIMIT 1) LIMIT 1), 'CURR-BSED-SCI-2026', 'BSEd Major in General Science CMO 75 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSEd' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'FIL' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSEd' LIMIT 1) LIMIT 1), 'CURR-BSED-FIL-2026', 'BSEd Major in Filipino CMO 75 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSEd' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'SS' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSEd' LIMIT 1) LIMIT 1), 'CURR-BSED-SS-2026', 'BSEd Major in Social Studies CMO 75 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BEEd' LIMIT 1), NULL, 'CURR-BEED-2026', 'Bachelor of Elementary Education CMO 74 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BECEd' LIMIT 1), NULL, 'CURR-BECED-2026', 'Bachelor of Early Childhood Education CMO 76 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSNEd' LIMIT 1), NULL, 'CURR-BSNED-2026', 'Bachelor of Special Needs Education CMO 77 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BPEd' LIMIT 1), NULL, 'CURR-BPED-2026', 'Bachelor of Physical Education CMO 80 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BTLEd' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'HE' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BTLEd' LIMIT 1) LIMIT 1), 'CURR-BTLED-HE-2026', 'BTLEd Major in Home Economics CMO 78 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BTLEd' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'IA' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BTLEd' LIMIT 1) LIMIT 1), 'CURR-BTLED-IA-2026', 'BTLEd Major in Industrial Arts CMO 78 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BTLEd' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'ICT' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BTLEd' LIMIT 1) LIMIT 1), 'CURR-BTLED-ICT-2026', 'BTLEd Major in ICT CMO 78 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BTVTEd' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'ET' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BTVTEd' LIMIT 1) LIMIT 1), 'CURR-BTVTED-ET-2026', 'BTVTEd Major in Electrical Technology CMO 79 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BTVTEd' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'ELX' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BTVTEd' LIMIT 1) LIMIT 1), 'CURR-BTVTED-ELX-2026', 'BTVTEd Major in Electronics Technology CMO 79 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BTVTEd' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'FSM' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BTVTEd' LIMIT 1) LIMIT 1), 'CURR-BTVTED-FSM-2026', 'BTVTEd Major in Food and Service Management CMO 79 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSCE' LIMIT 1), NULL, 'CURR-BSCE-2026', 'BS in Civil Engineering CMO 92 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSME' LIMIT 1), NULL, 'CURR-BSME-2026', 'BS in Mechanical Engineering CMO 97 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSEE' LIMIT 1), NULL, 'CURR-BSEE-2026', 'BS in Electrical Engineering CMO 88 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSECE' LIMIT 1), NULL, 'CURR-BSECE-2026', 'BS in Electronics Engineering CMO 101 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSCpE' LIMIT 1), NULL, 'CURR-BSCPE-2026', 'BS in Computer Engineering CMO 87 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSA' LIMIT 1), NULL, 'CURR-BSA-2026', 'BS in Accountancy CMO 27 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSMA' LIMIT 1), NULL, 'CURR-BSMA-2026', 'BS in Management Accounting CMO 27 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSBA' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'FM' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSBA' LIMIT 1) LIMIT 1), 'CURR-BSBA-FM-2026', 'BSBA Major in Financial Management CMO 28 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSBA' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'MM' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSBA' LIMIT 1) LIMIT 1), 'CURR-BSBA-MM-2026', 'BSBA Major in Marketing Management CMO 28 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSBA' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'HRM' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSBA' LIMIT 1) LIMIT 1), 'CURR-BSBA-HRM-2026', 'BSBA Major in Human Resource Management CMO 28 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BS-ENT' LIMIT 1), NULL, 'CURR-BSENT-2026', 'BS in Entrepreneurship CMO 17 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSHM' LIMIT 1), NULL, 'CURR-BSHM-2026', 'BS in Hospitality Management CMO 62 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSOA' LIMIT 1), NULL, 'CURR-BSOA-2026', 'BS in Office Administration CMO 18 s. 2017 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'AUTO' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1) LIMIT 1), 'CURR-BIT-AUTO-2026', 'BIT Major in Automotive Technology Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'COMP' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1) LIMIT 1), 'CURR-BIT-COMP-2026', 'BIT Major in Computer Technology Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'ELEC' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1) LIMIT 1), 'CURR-BIT-ELEC-2026', 'BIT Major in Electrical Technology Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'ELX' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1) LIMIT 1), 'CURR-BIT-ELX-2026', 'BIT Major in Electronics Technology Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'MECH' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1) LIMIT 1), 'CURR-BIT-MECH-2026', 'BIT Major in Mechanical Technology Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'DRAFT' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1) LIMIT 1), 'CURR-BIT-DRAFT-2026', 'BIT Major in Architectural Drafting Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'HVAC' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1) LIMIT 1), 'CURR-BIT-HVAC-2026', 'BIT Major in HVAC/R Technology Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'CUL' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1) LIMIT 1), 'CURR-BIT-CUL-2026', 'BIT Major in Culinary Technology Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1), (SELECT id FROM majors WHERE code COLLATE utf8mb4_0900_ai_ci = 'FASH' AND program_id = (SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BIT' LIMIT 1) LIMIT 1), 'CURR-BIT-FASH-2026', 'BIT Major in Fashion and Apparel Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSCRIM' LIMIT 1), NULL, 'CURR-CRIM-2026', 'BS in Criminology CMO 05 s. 2018 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE),
    ((SELECT id FROM programs WHERE code COLLATE utf8mb4_0900_ai_ci = 'BSFi' LIMIT 1), NULL, 'CURR-BSFI-2026', 'BS in Fisheries CMO 43 s. 2006 Curriculum', 'ACTIVE', 1, 'AY-2026-2027', TRUE)
AS new_curr ON DUPLICATE KEY UPDATE 
    major_id = new_curr.major_id,
    name = new_curr.name,
    status = 'ACTIVE',
    is_active = TRUE;

-- ============================================================================
-- 5. DETERMINISTIC STORED PROCEDURE: MapCurriculumCourse
-- ============================================================================

DROP PROCEDURE IF EXISTS MapCurriculumCourse;

DELIMITER $$
CREATE PROCEDURE MapCurriculumCourse(
    IN p_curr_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
    IN p_course_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
    IN p_year INT,
    IN p_sem VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
    IN p_is_major BOOLEAN
)
BEGIN
    DECLARE v_curr_id BIGINT;
    DECLARE v_course_id BIGINT;
    DECLARE v_credit_units DECIMAL(4, 2);
    DECLARE v_seq INT;
    DECLARE v_category VARCHAR(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

    SELECT id INTO v_curr_id FROM curricula WHERE code COLLATE utf8mb4_0900_ai_ci = p_curr_code COLLATE utf8mb4_0900_ai_ci LIMIT 1;
    SELECT id, credit_units INTO v_course_id, v_credit_units FROM courses WHERE code COLLATE utf8mb4_0900_ai_ci = p_course_code COLLATE utf8mb4_0900_ai_ci LIMIT 1;

    IF v_curr_id IS NOT NULL AND v_course_id IS NOT NULL THEN
        SELECT COALESCE(MAX(sequence_order), 0) + 1 INTO v_seq
        FROM curriculum_courses
        WHERE curriculum_id = v_curr_id AND year_level = p_year AND semester COLLATE utf8mb4_0900_ai_ci = p_sem COLLATE utf8mb4_0900_ai_ci;

        IF p_is_major THEN
            SET v_category = 'PROFESSIONAL_MAJOR';
        ELSE
            SELECT category INTO v_category FROM courses WHERE id = v_course_id;
            IF v_category IS NULL OR v_category = '' THEN
                SET v_category = 'GEN_ED';
            END IF;
        END IF;

        INSERT INTO curriculum_courses (curriculum_id, course_id, credit_units, year_level, semester, sequence_order, category)
        VALUES (v_curr_id, v_course_id, v_credit_units, p_year, p_sem, v_seq, v_category)
        ON DUPLICATE KEY UPDATE 
            credit_units = v_credit_units,
            year_level = p_year,
            semester = p_sem,
            category = v_category;
    END IF;
END $$
DELIMITER ;

-- ============================================================================
-- 6. EXHAUSTIVE 4-YEAR PROGRESSION MAPPINGS FOR ALL 47 CURRICULA & LEGACY CURRICULA
-- ============================================================================
-- Curriculum: CURR-BAEL-2026
CALL MapCurriculumCourse('CURR-BAEL-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'EL-INTRO', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'EL-LING1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'EL-LING2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'EL-SEM', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'EL-PRAG', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'EL-DISC', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'EL-CORPUS', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'CC101', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'EL-PSYCH', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'EL-COMM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'EL-INTER', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'ENG-LIT', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'EL-THES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'ENG-SPCH', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'ENG-MYTH', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'ENG-WRIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'BA-MAN', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'EL-THES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'ENG-STY', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'ENG-CAMP', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'BA-MKT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'CC105', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'BA-OPS', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'SS-COMM', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'EL-PRAC', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BAEL-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BASS-2026
CALL MapCurriculumCourse('CURR-BASS-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'SS-SOCIOL', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'SS-ANTHRO', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'SS-POLSCI', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'SS-GEOG', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'SS-HIST', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'SS-ASIAN', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'SS-STAT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'CC101', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'SS-DEV', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'SS-COMM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'SS-RES', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'PA-THEORY', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'SS-THES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'PA-ADMIN', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'PA-LOCAL', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'BA-MAN', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'PSY-SOCIAL', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'SS-THES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'PA-POLICY', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'BA-MICROE', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'BA-MACROE', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'CC105', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'BA-OPS', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'PA-PLAN', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'SS-PRAC', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BASS-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BPA-2026
CALL MapCurriculumCourse('CURR-BPA-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PA-THEORY', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PA-ADMIN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PA-HRM', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'SS-POLSCI', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PA-FISCAL', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PA-POLICY', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PA-LOCAL', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'CC101', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PA-ETHICS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PA-ORG', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PA-PLAN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'BA-MAN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PA-LAW', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PA-THES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'BA-MICROE', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'BA-MACROE', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'SS-STAT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PA-THES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'BA-OBLI', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'BA-BUSLAW', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'BA-OPS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'CC105', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'BA-STRAT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'SS-COMM', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'PA-PRAC', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPA-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSAM-2026
CALL MapCurriculumCourse('CURR-BSAM-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'MATH-ENG1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'CC101', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'MATH-ENG2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'CC102', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'MATH-DIFF', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'AM-CALC1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'AM-LINALG', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'CC103', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'AM-CALC2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'AM-DIFFEQ', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'AM-PROB', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'CC104', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'AM-NUM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'AM-OPTIM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'AM-COMPLEX', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'AM-DATA', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'AM-THES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'AM-THES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'AM-FINMATH', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'CC105', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'ENG-TECH', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'BA-MICROE', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'IT-AI', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'BA-OPS', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'IT-CLOUD', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'AM-PRAC', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSAM-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-PSYCH-2026
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-INTRO', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'SCI-BIO1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-DEV', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'SCI-BIO2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-STAT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-PERS', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-BIO', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'CC101', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-EXP', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-ABN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-COG', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-SOCIAL', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-IO', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-ASSESS', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-COUNSEL', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'ENG-TECH', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-THES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-THES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'BA-MAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'SS-SOCIOL', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'SS-ANTHRO', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'CC105', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'PSY-PRAC', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'BA-OPS', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-PSYCH-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSCS-2026
CALL MapCurriculumCourse('CURR-BSCS-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CC101', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-DISC', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CC102', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-ARCH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CC103', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CC105', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-AUTO', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'IT-NET1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CC104', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-PROGLANG', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-OS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CC106', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-ALGO', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-SE1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-AI', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-SEC', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-PAR', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-SE2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-DATA', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-GRAPH', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-COMP', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'IT-PROF', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-THES1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-MODEL', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-THES2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCS-2026', 'CS-PRAC', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSIT-2026
CALL MapCurriculumCourse('CURR-BSIT-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'CC101', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-DISC', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'CC102', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-NET1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'CC103', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'CC105', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-NET2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-WEB1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'CC104', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'CC106', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-WEB2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-SYSADM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-IAS1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-INT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-MOB', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-PROF', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IS-FUND', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-IAS2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-CLOUD', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-DATA', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IS-PROJ', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IS-SAD', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-CAP1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-AI', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IS-STRAT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-CAP2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIT-2026', 'IT-PRAC', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSIS-2026
CALL MapCurriculumCourse('CURR-BSIS-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'CC101', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IS-FUND', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'CC102', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'BA-MAN', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'CC103', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'CC105', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IS-EA', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'BA-MICROE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'CC104', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'CC106', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IS-BPM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'BA-MACROE', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IS-SAD', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IS-PROJ', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IT-NET1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IT-WEB1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'BA-OPS', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IS-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IT-IAS1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IS-AUDIT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IS-ERP', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IT-PROF', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IT-CAP1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'BA-STRAT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'BA-BUSLAW', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IT-CAP2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSIS-2026', 'IT-PRAC', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSED-MATH-2026
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'MATH-CALC1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'MATH-CALC2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'MATH-ALG1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'MATH-TRIG', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'MATH-GEOM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'MATH-STAT', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'MATH-NUM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'MATH-ALG2', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'MATH-MATHED', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'MATH-DIFFEQ', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'MATH-ASSESS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'MATH-RES', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'MATH-SEM', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-MATH-2026', 'CC101', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSED-ENG-2026
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ENG-LING', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ENG-STRUK', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ENG-LIT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ENG-SPCH', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ENG-MYTH', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ENG-WRIT', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ENG-SEM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ENG-STY', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ENG-TRANS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ENG-CHILD', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ENG-REMED', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ENG-RES', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ENG-CAMP', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-ENG-2026', 'CC101', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSED-SCI-2026
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'SCI-BIO1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'SCI-BIO2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'SCI-CHEM1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'SCI-PHYS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'SCI-CHEM2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'SCI-PHYS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'SCI-EARTH', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'SCI-ASTR', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'SCI-ECOL', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'SCI-ORG', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'SCI-MICRO', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'SCI-GENET', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'SCI-METHO', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'SCI-RES', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SCI-2026', 'GEC-ELE4', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSED-FIL-2026
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'FIL-INTRO', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'FIL-ESTRUK', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'FIL-PANIT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'FIL-RETOR', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'FIL-WIKA', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'FIL-TULA', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'FIL-DULA', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'FIL-MAIKLI', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'FIL-SANAY', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'FIL-TRANS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'FIL-KULT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'FIL-REMED', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'FIL-SALIK', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-FIL-2026', 'CC101', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSED-SS-2026
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'SS-GEOG', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'SS-ASIAN', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'SS-HIST', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'SS-SOCIOL', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'SS-ANTHRO', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'SS-POLSCI', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'SS-DEV', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'SS-RES', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'SS-TRENDS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'SS-TEACH', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'SS-MAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'SS-THES1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'SS-THES2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSED-SS-2026', 'CC101', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BEED-2026
CALL MapCurriculumCourse('CURR-BEED-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'EED-CONTENT1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'EED-CONTENT2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'EED-MOTHER', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'EED-MATH1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'EED-ART', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'EED-MATH2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'EED-SCI1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'EED-ENG', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'EED-SCI2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'EED-FIL', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'EED-VAL', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'EED-PE', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'EED-RES', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BEED-2026', 'CC101', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BECED-2026
CALL MapCurriculumCourse('CURR-BECED-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ECED-DEV', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ECED-PLAY', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ECED-CURR', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ECED-LIT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ECED-NUM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ECED-INCL', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ECED-HEALTH', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ECED-ENV', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ECED-FAM', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ECED-ASSESS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ECED-INFANT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ECED-MUSIC', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ECED-RES', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BECED-2026', 'CC101', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSNED-2026
CALL MapCurriculumCourse('CURR-BSNED-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'SNED-INCL', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'SNED-BRAILLE', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'SNED-SIGN', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'SNED-AUTISM', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'SNED-GIFTED', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'SNED-ASSESS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'SNED-BEHAV', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'SNED-INTEL', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'SNED-PHYS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'SNED-TRANS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'SNED-CURR', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'SNED-COMM', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'SNED-RES', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSNED-2026', 'CC101', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BPED-2026
CALL MapCurriculumCourse('CURR-BPED-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PED-ANAT', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PED-PHYS', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PED-MOVE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PED-GAMES', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PED-DANCE', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PED-COACH', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PED-TEAM1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PED-TEAM2', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PED-SWIM', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PED-GYM', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PED-FIT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PED-ADM', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'PED-RES', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BPED-2026', 'CC101', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BTLED-HE-2026
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'TLE-HE1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'TLE-HE2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'TLE-HE3', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'TLE-HE4', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'TLE-HE5', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'TLE-HE6', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'TLE-HE7', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'TLE-HE8', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'TLE-HE9', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'TLE-HE10', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'TLE-HE11', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'TLE-HE12', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'BIT-MGT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-HE-2026', 'BIT-SHOP', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BTLED-IA-2026
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'TLE-IA1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'TLE-IA2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'TLE-IA3', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'TLE-IA4', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'TLE-IA5', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'TLE-IA6', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'TLE-IA7', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'TLE-IA8', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'TLE-IA9', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'TLE-IA10', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'TLE-IA11', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'TLE-IA12', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'BIT-MGT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-IA-2026', 'BIT-SHOP', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BTLED-ICT-2026
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ICT-HARD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ICT-NET', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ICT-PROG', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ICT-WEB', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ICT-MEDIA', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'CC101', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ICT-PEDAG', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'CC102', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'CC103', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'CC105', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'IT-NET1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'IT-WEB1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'BIT-MGT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTLED-ICT-2026', 'BIT-SHOP', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BTVTED-ET-2026
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'TVED-ELEC1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'TVED-ELEC2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'TVED-ELEC3', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'TVED-ELEC4', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'TVED-ELEC5', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'TVED-ELEC6', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'TVED-ELEC7', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'TVED-ELEC8', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'TVED-ELEC9', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'TVED-ELEC10', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'TVED-ELEC11', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'TVED-ELEC12', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'BIT-MGT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ET-2026', 'BIT-SHOP', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BTVTED-ELX-2026
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'TVED-ELX1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'TVED-ELX2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'TVED-ELX3', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'TVED-ELX4', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'TVED-ELX5', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'TVED-ELX6', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'TVED-ELX7', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'TVED-ELX8', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'TVED-ELX9', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'TVED-ELX10', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'TVED-ELX11', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'TVED-ELX12', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'BIT-MGT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-ELX-2026', 'BIT-SHOP', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BTVTED-FSM-2026
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'FSM-SAN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'FSM-BAKE', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'FSM-COOK1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'FSM-COST', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'FSM-COOK2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'BIT-CUL1', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'FSM-BAR', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'FSM-CATER', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'FSM-PEDAG', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'HM-KITCHEN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'HM-FB', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'BIT-MGT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'BIT-SHOP', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BTVTED-FSM-2026', 'HM-PRAC', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSCE-2026
CALL MapCurriculumCourse('CURR-BSCE-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'MATH-ENG1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'ENG-CHEM', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'MATH-ENG2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'ENG-PHYS1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'MATH-DIFF', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'ENG-PHYS2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'ENG-STAT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-SURV1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'ENG-DYN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'ENG-DEFORM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-SURV2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-HYDRO', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-STRUCT1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-GEOTECH', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-WATER', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'ENG-ECON', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'ENG-MGT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-STRUCT2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-CONC', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-STEEL', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-HIGHWAY', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'BIT-DRAW2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-DES1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-CONST', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-FOUND', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-DES2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCE-2026', 'CE-OJT', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSME-2026
CALL MapCurriculumCourse('CURR-BSME-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'MATH-ENG1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ENG-CHEM', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'MATH-ENG2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ENG-PHYS1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'MATH-DIFF', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ENG-PHYS2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ENG-STAT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ME-THERMO1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ENG-DYN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ENG-DEFORM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ME-THERMO2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ME-FLUID', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ME-MACH1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ME-HEAT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ME-MAT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ENG-ECON', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ENG-MGT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ME-MACH2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ME-POWER', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ME-RAC', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ME-MFG', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'EE-CIRC1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ME-DES1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ME-AUTO', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'BIT-DRAW2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ME-DES2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSME-2026', 'ME-OJT', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSEE-2026
CALL MapCurriculumCourse('CURR-BSEE-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'MATH-ENG1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'ENG-CHEM', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'MATH-ENG2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'ENG-PHYS1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'MATH-DIFF', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'ENG-PHYS2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'ENG-STAT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'EE-CIRC1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'ENG-DYN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'ENG-DEFORM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'EE-CIRC2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'ECE-ELEC1', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'EE-MACH1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'EE-POWER1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'EE-ILLUM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'ENG-ECON', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'ENG-MGT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'EE-MACH2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'EE-POWER2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'EE-DISTRIB', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'EE-CONTROL', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'EE-CODE', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'EE-DES1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'TVED-ELEC3', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'CPE-LOGIC', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'EE-DES2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSEE-2026', 'EE-OJT', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSECE-2026
CALL MapCurriculumCourse('CURR-BSECE-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'MATH-ENG1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ENG-CHEM', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'MATH-ENG2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ENG-PHYS1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'MATH-DIFF', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ENG-PHYS2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ENG-STAT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'EE-CIRC1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ENG-DYN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ENG-DEFORM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ECE-ELEC1', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'CPE-LOGIC', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ECE-ELEC2', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ECE-COMM1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ECE-SIGNALS', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ENG-ECON', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ENG-MGT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ECE-COMM2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ECE-EMAG', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ECE-WIRELESS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'CPE-EMBED', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ECE-LAWS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ECE-DES1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'IT-NET1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'IT-IAS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ECE-DES2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSECE-2026', 'ECE-OJT', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSCPE-2026
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'MATH-ENG1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CC101', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'MATH-ENG2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CC102', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CC103', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'ENG-PHYS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CPE-LOGIC', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'MATH-DIFF', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CC104', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'ENG-PHYS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CPE-ARCH', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'EE-CIRC1', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CPE-EMBED', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CPE-OS', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CPE-NET', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'ENG-ECON', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'ENG-MGT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CPE-DSP', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CPE-SEC', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'ECE-ELEC1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CC105', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'IT-NET2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CPE-DES1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'IT-CLOUD', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'IT-AI', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CPE-DES2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSCPE-2026', 'CPE-OJT', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSA-2026
CALL MapCurriculumCourse('CURR-BSA-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'BA-MAN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-FAR', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'BA-MKT', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-CFAS', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'BA-MICROE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'BA-OBLI', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-IA1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-COST', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'BA-MACROE', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'BA-BUSLAW', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-IA2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-SCM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'BA-TAX1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-IA3', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-AFAR1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-AUD1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'FM-FINMAN', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'BA-TAX2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'BA-OPS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-AFAR2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-AUD2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-AUDCIS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'BA-STRAT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-INTEG1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-INTEG2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'ACT-INT', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSA-2026', 'CC101', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSMA-2026
CALL MapCurriculumCourse('CURR-BSMA-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'BA-MAN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'ACT-FAR', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'BA-MKT', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'ACT-CFAS', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'BA-MICROE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'BA-OBLI', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'ACT-IA1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'ACT-COST', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'BA-MACROE', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'BA-BUSLAW', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'ACT-IA2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'MA-COST1', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'BA-TAX1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'ACT-SCM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'MA-COST2', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'FM-FINMAN', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'MA-QUANT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'BA-TAX2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'BA-OPS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'MA-PERF', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'ACT-AUD1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'MA-FIN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'BA-STRAT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'MA-AUDIT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'FM-INVEST', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'MA-INT', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSMA-2026', 'CC101', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSBA-FM-2026
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'BA-MAN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'ACT-FAR', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'BA-MKT', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'BA-MICROE', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'BA-MACROE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'BA-OBLI', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'ACT-COST', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'FM-FINMAN', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'BA-BUSLAW', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'BA-TAX1', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'FM-MARKET', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'FM-INVEST', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'BA-TAX2', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'BA-OPS', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'FM-CREDIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'FM-PUBLIC', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'ENT-OPP', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'FM-GLOBAL', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'MA-COST1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'ACT-CFAS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'CC101', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'ENT-PLAN', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'CC105', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'IT-WEB1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'FM-INT', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-FM-2026', 'IS-FUND', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSBA-MM-2026
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'BA-MAN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'BA-MKT', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'BA-MICROE', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'MM-BEHAV', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'BA-MACROE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'BA-OBLI', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'MM-RES', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'MM-COMM', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'BA-BUSLAW', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'BA-OPS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'MM-PRIC', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'MM-DIST', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'BA-TAX1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'MM-DIG', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'MM-SALES', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'MM-SERV', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'ACT-FAR', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'BA-TAX2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'MM-BRAND', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'MM-INT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'MM-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'FM-FINMAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'BA-STRAT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'MM-PLAN', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'ENT-OPP', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'CC105', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'FM-INT', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-MM-2026', 'IS-FUND', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSBA-HRM-2026
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'BA-MAN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'HR-ADMIN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'BA-MICROE', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'HR-RECRUIT', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'BA-MACROE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'BA-OBLI', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'HR-TRAIN', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'HR-COMP', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'BA-BUSLAW', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'BA-OPS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'HR-REL', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'HR-PERF', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'BA-TAX1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'HR-ORG', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'HR-ANALYT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'HR-HEALTH', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'ACT-FAR', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'BA-TAX2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'HR-LAW', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'HR-GLOBAL', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'HR-ETHICS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'FM-FINMAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'BA-STRAT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'HR-PLAN', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'CC105', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'FM-INT', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSBA-HRM-2026', 'IS-FUND', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSENT-2026
CALL MapCurriculumCourse('CURR-BSENT-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'BA-MAN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'ENT-OPP', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'BA-MKT', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'ACT-FAR', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'BA-MICROE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'BA-OBLI', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'ENT-PLAN', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'ENT-INNOV', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'BA-MACROE', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'BA-BUSLAW', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'BA-TAX1', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'ENT-FIN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'BA-OPS', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'BA-TAX2', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'ENT-GROWTH', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'FM-FINMAN', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'CC101', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'ENT-SCALE', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'BIT-MGT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'CC105', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'FM-MARKET', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'IT-WEB1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'OA-COMM', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'IS-FUND', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'ENT-INT', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSENT-2026', 'IS-PROJ', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSHM-2026
CALL MapCurriculumCourse('CURR-BSHM-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'BA-MAN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'HM-KITCHEN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'BA-MKT', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'HM-FB', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'BA-MICROE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'BA-OBLI', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'HM-ROOMS', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'HM-EVENT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'BA-MACROE', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'BA-TAX1', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'HM-BAR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'HM-CULINARY', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'BA-OPS', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'BA-BUSLAW', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'HM-TOUR', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'ACT-FAR', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'BIT-SHOP', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'ENT-OPP', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'OA-COMM', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'CC101', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'FM-FINMAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'ENT-PLAN', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'OA-PROC', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'BIT-MGT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'HM-PRAC', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSHM-2026', 'IS-FUND', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSOA-2026
CALL MapCurriculumCourse('CURR-BSOA-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'BA-MAN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'OA-KEY', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'BA-MKT', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'OA-PROC', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'BA-MICROE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'BA-OBLI', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'OA-RECORDS', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'OA-COMM', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'BA-MACROE', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'BA-BUSLAW', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'OA-SHORTHAND', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'OA-DESK', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'BA-TAX1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'BA-OPS', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'ACT-FAR', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'CC101', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'IS-FUND', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'FM-FINMAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'ENT-OPP', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'CC105', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'IT-WEB1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'ENT-PLAN', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'IS-PROJ', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'IT-PROF', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'OA-EXEC', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSOA-2026', 'IS-EA', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BIT-AUTO-2026
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BIT-DRAW1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BIT-SHOP', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BIT-DRAW2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BIT-AUTO1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BIT-MGT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BIT-AUTO2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'CC101', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'ENG-TECH', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BIT-AUTO3', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BIT-AUTO4', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BA-MAN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BA-OPS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BIT-AUTO5', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BIT-RES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'CC105', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BA-MKT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'ENT-OPP', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BIT-AUTO6', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BIT-RES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'ENT-PLAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'IT-WEB1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BIT-OJT1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'BIT-OJT2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-AUTO-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BIT-COMP-2026
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BIT-DRAW1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BIT-SHOP', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BIT-DRAW2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BIT-COMP1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BIT-MGT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BIT-COMP2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'CC101', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'ENG-TECH', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BIT-COMP3', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BIT-COMP4', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BA-MAN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BA-OPS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BIT-COMP5', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BIT-RES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'CC105', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BA-MKT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'ENT-OPP', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BIT-COMP6', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BIT-RES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'ENT-PLAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'IT-WEB1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BIT-OJT1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'BIT-OJT2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-COMP-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BIT-ELEC-2026
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BIT-DRAW1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BIT-SHOP', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BIT-DRAW2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BIT-ELEC1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BIT-MGT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BIT-ELEC2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'CC101', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'ENG-TECH', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BIT-ELEC3', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BIT-ELEC4', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BA-MAN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BA-OPS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BIT-ELEC5', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BIT-RES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'CC105', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BA-MKT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'ENT-OPP', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BIT-ELEC6', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BIT-RES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'ENT-PLAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'IT-WEB1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BIT-OJT1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'BIT-OJT2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELEC-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BIT-ELX-2026
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BIT-DRAW1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BIT-SHOP', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BIT-DRAW2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BIT-ELX1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BIT-MGT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BIT-ELX2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'CC101', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'ENG-TECH', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BIT-ELX3', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BIT-ELX4', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BA-MAN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BA-OPS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BIT-ELX5', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BIT-RES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'CC105', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BA-MKT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'ENT-OPP', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BIT-ELX6', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BIT-RES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'ENT-PLAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'IT-WEB1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BIT-OJT1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'BIT-OJT2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-ELX-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BIT-MECH-2026
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BIT-DRAW1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BIT-SHOP', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BIT-DRAW2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BIT-MECH1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BIT-MGT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BIT-MECH2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'CC101', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'ENG-TECH', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BIT-MECH3', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BIT-MECH4', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BA-MAN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BA-OPS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BIT-MECH5', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BIT-RES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'CC105', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BA-MKT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'ENT-OPP', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BIT-MECH6', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BIT-RES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'ENT-PLAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'IT-WEB1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BIT-OJT1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'BIT-OJT2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-MECH-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BIT-DRAFT-2026
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BIT-DRAW1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BIT-SHOP', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BIT-DRAW2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BIT-DRAFT1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BIT-MGT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BIT-DRAFT2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'CC101', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'ENG-TECH', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BIT-DRAFT3', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BIT-DRAFT4', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BA-MAN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BA-OPS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BIT-DRAFT5', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BIT-RES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'CC105', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BA-MKT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'ENT-OPP', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BIT-DRAFT6', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BIT-RES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'ENT-PLAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'IT-WEB1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BIT-OJT1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'BIT-OJT2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-DRAFT-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BIT-HVAC-2026
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BIT-DRAW1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BIT-SHOP', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BIT-DRAW2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BIT-HVAC1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BIT-MGT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BIT-HVAC2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'CC101', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'ENG-TECH', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BIT-HVAC3', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BIT-HVAC4', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BA-MAN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BA-OPS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BIT-HVAC5', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BIT-RES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'CC105', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BA-MKT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'ENT-OPP', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BIT-HVAC6', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BIT-RES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'ENT-PLAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'IT-WEB1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BIT-OJT1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'BIT-OJT2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-HVAC-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BIT-CUL-2026
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BIT-DRAW1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BIT-SHOP', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BIT-DRAW2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BIT-CUL1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BIT-MGT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BIT-CUL2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'CC101', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'ENG-TECH', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BIT-CUL3', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BIT-CUL4', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BA-MAN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BA-OPS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BIT-CUL5', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BIT-RES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'CC105', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BA-MKT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'ENT-OPP', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BIT-CUL6', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BIT-RES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'ENT-PLAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'IT-WEB1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BIT-OJT1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'BIT-OJT2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-CUL-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BIT-FASH-2026
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BIT-DRAW1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BIT-SHOP', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BIT-DRAW2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BIT-FASH1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BIT-MGT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BIT-FASH2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'CC101', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'ENG-TECH', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BIT-FASH3', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BIT-FASH4', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BA-MAN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BA-OPS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BIT-FASH5', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BIT-RES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'CC105', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BA-MKT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'ENT-OPP', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BIT-FASH6', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BIT-RES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'ENT-PLAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'IT-WEB1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BIT-OJT1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'BIT-OJT2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BIT-FASH-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-CRIM-2026
CALL MapCurriculumCourse('CURR-CRIM-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-INTRO', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-LAW1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-LEA1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-DEF', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-LAW2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-PHOTO', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-FINGER', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-MARK', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-PROC', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-BALL', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-POLY', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-DOC', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-LEA2', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-CORR1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-INV1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-TRAF', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-THES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-CORR2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-INV2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-DRUG', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-THES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CC101', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-PRAC1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CC105', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'BA-MAN', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'CRIM-PRAC2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-CRIM-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: CURR-BSFI-2026
CALL MapCurriculumCourse('CURR-BSFI-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'FISH-INTRO', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'SCI-BIO1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'FISH-AQUA1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'SCI-BIO2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'FISH-AQUA2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'FISH-CAPTURE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'SCI-CHEM1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'ENG-PHYS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'FISH-POST', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'FISH-OCEAN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'FISH-ECOL', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'SCI-CHEM2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'FISH-HEALTH', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'FISH-NUTR', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'FISH-HATCH', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'FISH-THES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'CC101', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'FISH-THES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'FISH-LAWS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'BA-MAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'BA-MKT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'ENT-OPP', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'FISH-PRAC', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'ENT-PLAN', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('CURR-BSFI-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- Curriculum: BSCS-2026
CALL MapCurriculumCourse('BSCS-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'CC101', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-DISC', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'CC102', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-ARCH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'CC103', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CC105', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-AUTO', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'IT-NET1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'CC104', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-PROGLANG', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-OS', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CC106', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-ALGO', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-SE1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-AI', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-SEC', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-PAR', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-SE2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-DATA', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-GRAPH', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-COMP', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'IT-PROF', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-THES1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-MODEL', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-THES2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCS-2026', 'CS-PRAC', 4, '2ND_SEM', TRUE);

-- Curriculum: BSIT-2026
CALL MapCurriculumCourse('BSIT-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'CC101', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-DISC', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'CC102', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-NET1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'CC103', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'CC105', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-NET2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-WEB1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'CC104', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'CC106', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-WEB2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-SYSADM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-IAS1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-INT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-MOB', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-PROF', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IS-FUND', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-IAS2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-CLOUD', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-DATA', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IS-PROJ', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IS-SAD', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-CAP1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-AI', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IS-STRAT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-CAP2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIT-2026', 'IT-PRAC', 4, '2ND_SEM', TRUE);

-- Curriculum: BSIS-2026
CALL MapCurriculumCourse('BSIS-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'CC101', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'IS-FUND', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'CC102', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'BA-MAN', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'CC103', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'CC105', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'IS-EA', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'BA-MICROE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'CC104', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'CC106', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'IS-BPM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'BA-MACROE', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'IS-SAD', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'IS-PROJ', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'IT-NET1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'IT-WEB1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'BA-OPS', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSIS-2026', 'IS-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'IT-IAS1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'IS-AUDIT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'IS-ERP', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'IT-PROF', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'IT-CAP1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'BA-STRAT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'BA-BUSLAW', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'IT-CAP2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSIS-2026', 'IT-PRAC', 4, '2ND_SEM', TRUE);

-- Curriculum: BSCpE-2026
CALL MapCurriculumCourse('BSCpE-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'MATH-ENG1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'CC101', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'MATH-ENG2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'CC102', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'CC103', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'ENG-PHYS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'CPE-LOGIC', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'MATH-DIFF', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'CC104', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'ENG-PHYS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'CPE-ARCH', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'EE-CIRC1', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'CPE-EMBED', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'CPE-OS', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'CPE-NET', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'ENG-ECON', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'ENG-MGT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCpE-2026', 'CPE-DSP', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'CPE-SEC', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'ECE-ELEC1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'CC105', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'IT-NET2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'CPE-DES1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'IT-CLOUD', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'IT-AI', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'CPE-DES2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCpE-2026', 'CPE-OJT', 4, '2ND_SEM', TRUE);

-- Curriculum: BSECE-2026
CALL MapCurriculumCourse('BSECE-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'MATH-ENG1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ENG-CHEM', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'MATH-ENG2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ENG-PHYS1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'MATH-DIFF', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ENG-PHYS2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ENG-STAT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'EE-CIRC1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'ENG-DYN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ENG-DEFORM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ECE-ELEC1', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'CPE-LOGIC', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'ECE-ELEC2', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ECE-COMM1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ECE-SIGNALS', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ENG-ECON', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ENG-MGT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSECE-2026', 'ECE-COMM2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ECE-EMAG', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ECE-WIRELESS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'CPE-EMBED', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ECE-LAWS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ECE-DES1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'IT-NET1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'IT-IAS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ECE-DES2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSECE-2026', 'ECE-OJT', 4, '2ND_SEM', TRUE);

-- Curriculum: BSCE-2026
CALL MapCurriculumCourse('BSCE-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'MATH-ENG1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'ENG-CHEM', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'MATH-ENG2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'ENG-PHYS1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'MATH-DIFF', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'ENG-PHYS2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'ENG-STAT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-SURV1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'ENG-DYN', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'ENG-DEFORM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-SURV2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-HYDRO', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-STRUCT1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-GEOTECH', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-WATER', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'ENG-ECON', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'ENG-MGT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-STRUCT2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-CONC', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-STEEL', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-HIGHWAY', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'BIT-DRAW2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-DES1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-CONST', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-FOUND', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-DES2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCE-2026', 'CE-OJT', 4, '2ND_SEM', TRUE);

-- Curriculum: BSBA-FM-2026
CALL MapCurriculumCourse('BSBA-FM-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'BA-MAN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'ACT-FAR', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'BA-MKT', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'BA-MICROE', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'BA-MACROE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'BA-OBLI', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'ACT-COST', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'FM-FINMAN', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'BA-BUSLAW', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'BA-TAX1', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'FM-MARKET', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'FM-INVEST', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'BA-TAX2', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'BA-OPS', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'FM-CREDIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'FM-PUBLIC', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'ENT-OPP', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'FM-GLOBAL', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'MA-COST1', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'ACT-CFAS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'CC101', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'ENT-PLAN', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'CC105', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'IT-WEB1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'FM-INT', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSBA-FM-2026', 'IS-FUND', 4, '2ND_SEM', TRUE);

-- Curriculum: BSA-2026
CALL MapCurriculumCourse('BSA-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'BA-MAN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-FAR', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'BA-MKT', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-CFAS', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'BA-MICROE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'BA-OBLI', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-IA1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-COST', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'BA-MACROE', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'BA-BUSLAW', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-IA2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-SCM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'BA-TAX1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-IA3', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-AFAR1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-AUD1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'FM-FINMAN', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSA-2026', 'BA-TAX2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'BA-OPS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-AFAR2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-AUD2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-AUDCIS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'BA-STRAT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-INTEG1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-INTEG2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'ACT-INT', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSA-2026', 'CC101', 4, '2ND_SEM', TRUE);

-- Curriculum: BSHM-2026
CALL MapCurriculumCourse('BSHM-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'BA-MAN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'HM-KITCHEN', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'BA-MKT', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'HM-FB', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'BA-MICROE', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'BA-OBLI', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'HM-ROOMS', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'HM-EVENT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'BA-MACROE', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'BA-TAX1', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'HM-BAR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'HM-CULINARY', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'BA-OPS', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'BA-BUSLAW', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'HM-TOUR', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'ACT-FAR', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'BIT-SHOP', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSHM-2026', 'BA-STRAT', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'ENT-OPP', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'OA-COMM', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'CC101', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'FM-FINMAN', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'ENT-PLAN', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'OA-PROC', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'BIT-MGT', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'HM-PRAC', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSHM-2026', 'IS-FUND', 4, '2ND_SEM', TRUE);

-- Curriculum: BSED-MATH-2026
CALL MapCurriculumCourse('BSED-MATH-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'MATH-CALC1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'MATH-CALC2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'MATH-ALG1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'MATH-TRIG', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'MATH-GEOM', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'MATH-STAT', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'MATH-NUM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'MATH-ALG2', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'MATH-MATHED', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'MATH-DIFFEQ', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'MATH-ASSESS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'MATH-RES', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'MATH-SEM', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-MATH-2026', 'CC101', 4, '2ND_SEM', TRUE);

-- Curriculum: BSED-ENG-2026
CALL MapCurriculumCourse('BSED-ENG-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ENG-LING', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ENG-STRUK', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ENG-LIT', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ENG-SPCH', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ENG-MYTH', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ENG-WRIT', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ENG-SEM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ENG-STY', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ENG-TRANS', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ENG-CHILD', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ENG-REMED', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ENG-RES', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ENG-CAMP', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSED-ENG-2026', 'CC101', 4, '2ND_SEM', TRUE);

-- Curriculum: BEED-2026
CALL MapCurriculumCourse('BEED-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'ED-CHILD', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'EED-CONTENT1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'ED-TEACH', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'EED-CONTENT2', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'ED-SPED', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'ED-ASS1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'EED-MOTHER', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'EED-MATH1', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'ED-ASS2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'ED-CURR', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'EED-ART', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'EED-MATH2', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'ED-COMM', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'ED-TTL1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'ED-LIT', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'EED-SCI1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'EED-ENG', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BEED-2026', 'ED-TTL2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'ED-RES', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'EED-SCI2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'EED-FIL', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'EED-VAL', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'ED-FS1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'ED-FS2', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'EED-PE', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'EED-RES', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'ED-TI', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BEED-2026', 'CC101', 4, '2ND_SEM', TRUE);

-- Curriculum: BSCRIM-2026
CALL MapCurriculumCourse('BSCRIM-2026', 'GEC-UTS', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'GEC-RPH', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'GEC-MMW', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'PE-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'NSTP-1', 1, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-INTRO', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-LAW1', 1, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'GEC-TCW', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'GEC-PC', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'GEC-ART', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'PE-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'NSTP-2', 1, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-LEA1', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-DEF', 1, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'GEC-STS', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'GEC-ETH', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'PE-3', 2, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-LAW2', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-PHOTO', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-FINGER', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-MARK', 2, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'GEC-ELE1', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'GEC-ELE2', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'PE-4', 2, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-PROC', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-BALL', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-POLY', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-DOC', 2, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'RIZAL', 3, '1ST_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-LEA2', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-CORR1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-INV1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-TRAF', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-THES1', 3, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'GEC-ELE3', 3, '2ND_SEM', FALSE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-CORR2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-INV2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-DRUG', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-THES2', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CC101', 3, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-PRAC1', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CC105', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'ENG-TECH', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'BA-MAN', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'GEC-ELE4', 4, '1ST_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'CRIM-PRAC2', 4, '2ND_SEM', TRUE);
CALL MapCurriculumCourse('BSCRIM-2026', 'IT-PROF', 4, '2ND_SEM', TRUE);

-- ============================================================================
-- 7. CLEANUP
-- ============================================================================

DROP PROCEDURE IF EXISTS MapCurriculumCourse;

SET FOREIGN_KEY_CHECKS = 1;
