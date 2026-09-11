-- V5__seed_bootstrap_users_and_fixtures.sql
-- Subsystem: User Accounts, Personas, Leadership Scoping, and Operational Fixtures
-- Scope: Bootstrap Users, Scoped Role Assignments, Faculty Profiles, Rooms, Student Profiles, Historical Grades

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. Bootstrap User Accounts (Standard Argon2id Password: Password123!)
-- -----------------------------------------------------------------------------
INSERT INTO users (id, username, email, password, enabled, college_id, program_id, created_at)
VALUES
(1, 'admin', 'admin@example.com', '$argon2id$v=19$m=16384,t=2,p=1$Ed3PWKV7XMiHtZvKD7Wu1w$iNk1epo/wYCSevNZgCwCGVXGwlx0Lq42vYOuQ0Ki7kg', TRUE, NULL, NULL, NOW()),
(2, 'dean_morris', 'dean.morris@example.com', '$argon2id$v=19$m=16384,t=2,p=1$Ed3PWKV7XMiHtZvKD7Wu1w$iNk1epo/wYCSevNZgCwCGVXGwlx0Lq42vYOuQ0Ki7kg', TRUE, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), NULL, NOW()),
(3, 'chair_clark', 'chair.clark@example.com', '$argon2id$v=19$m=16384,t=2,p=1$Ed3PWKV7XMiHtZvKD7Wu1w$iNk1epo/wYCSevNZgCwCGVXGwlx0Lq42vYOuQ0Ki7kg', TRUE, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), NOW()),
(4, 'faculty_alice', 'faculty.alice@example.com', '$argon2id$v=19$m=16384,t=2,p=1$Ed3PWKV7XMiHtZvKD7Wu1w$iNk1epo/wYCSevNZgCwCGVXGwlx0Lq42vYOuQ0Ki7kg', TRUE, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), NOW()),
(5, 'registrar_bob', 'registrar.bob@example.com', '$argon2id$v=19$m=16384,t=2,p=1$Ed3PWKV7XMiHtZvKD7Wu1w$iNk1epo/wYCSevNZgCwCGVXGwlx0Lq42vYOuQ0Ki7kg', TRUE, NULL, NULL, NOW()),
(6, 'student_john', 'student.john@example.com', '$argon2id$v=19$m=16384,t=2,p=1$Ed3PWKV7XMiHtZvKD7Wu1w$iNk1epo/wYCSevNZgCwCGVXGwlx0Lq42vYOuQ0Ki7kg', TRUE, NULL, NULL, NOW()),
(7, 'inactive_user', 'disabled@example.com', '$argon2id$v=19$m=16384,t=2,p=1$Ed3PWKV7XMiHtZvKD7Wu1w$iNk1epo/wYCSevNZgCwCGVXGwlx0Lq42vYOuQ0Ki7kg', FALSE, NULL, NULL, NOW())
ON DUPLICATE KEY UPDATE email = VALUES(email),
                        password = VALUES(password),
                        enabled = VALUES(enabled),
                        college_id = VALUES(college_id),
                        program_id = VALUES(program_id);

-- -----------------------------------------------------------------------------
-- 2. Role Assignments
-- -----------------------------------------------------------------------------
INSERT INTO user_roles (user_id, role)
VALUES
(1, 'ADMIN'),
(2, 'DEAN'),
(2, 'FACULTY'),
(3, 'CHAIRPERSON'),
(3, 'FACULTY'),
(4, 'FACULTY'),
(5, 'REGISTRAR'),
(6, 'STUDENT'),
(7, 'FACULTY')
ON DUPLICATE KEY UPDATE role = VALUES(role);

-- -----------------------------------------------------------------------------
-- 3. Academic Leadership Bindings
-- -----------------------------------------------------------------------------
-- Dean Morris assigned to College of Computer Studies (CCS)
UPDATE departments 
SET dean_user_id = 2 
WHERE code = 'CCS';

-- Chair Clark assigned to BSIT Program
UPDATE programs 
SET chairperson_user_id = 3 
WHERE code = 'BSIT';

-- -----------------------------------------------------------------------------
-- 4. Faculty Profiles & Credentials
-- -----------------------------------------------------------------------------
INSERT INTO faculty_profiles (user_id, college_id, program_id, faculty_id_number, highest_degree, academic_rank, prc_license_no, employment_status, is_tenured)
VALUES
(2, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), NULL, 'FAC-2026-0002', 'DOCTORATE', 'PROFESSOR_I', 'PRC-0098765', 'FULL_TIME', TRUE),
(3, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'FAC-2026-0003', 'MASTERS', 'ASSOCIATE_PROFESSOR_I', 'PRC-0054321', 'FULL_TIME', TRUE),
(4, (SELECT id FROM departments WHERE code = 'CCS' LIMIT 1), (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1), 'FAC-2026-0004', 'MASTERS', 'INSTRUCTOR_I', 'PRC-0012345', 'FULL_TIME', TRUE)
ON DUPLICATE KEY UPDATE college_id = VALUES(college_id),
                        program_id = VALUES(program_id),
                        highest_degree = VALUES(highest_degree),
                        academic_rank = VALUES(academic_rank),
                        prc_license_no = VALUES(prc_license_no),
                        employment_status = VALUES(employment_status),
                        is_tenured = VALUES(is_tenured);

-- -----------------------------------------------------------------------------
-- 5. Physical Facilities (Talisay Main Campus)
-- -----------------------------------------------------------------------------
INSERT INTO rooms (id, campus_id, code, name, building, floor, capacity, room_type, is_active)
VALUES
(1, 1, 'TAL-IT-LAB1', 'Computer Laboratory 1', 'Engineering & Tech Building', 2, 40, 'LABORATORY', TRUE),
(2, 1, 'TAL-IT-LAB2', 'Computer Laboratory 2', 'Engineering & Tech Building', 2, 40, 'LABORATORY', TRUE),
(3, 1, 'TAL-ENG-301', 'Engineering Lecture Hall 301', 'Engineering & Tech Building', 3, 50, 'LECTURE', TRUE),
(4, 1, 'TAL-ENG-302', 'Engineering Lecture Hall 302', 'Engineering & Tech Building', 3, 50, 'LECTURE', TRUE),
(5, 1, 'TAL-GEN-101', 'General Education Hall 101', 'Academic Building', 1, 45, 'LECTURE', TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name),
                        building = VALUES(building),
                        floor = VALUES(floor),
                        capacity = VALUES(capacity),
                        room_type = VALUES(room_type),
                        is_active = VALUES(is_active);

-- -----------------------------------------------------------------------------
-- 6. Student Profile for student_john (2nd Year BSIT Regular)
-- -----------------------------------------------------------------------------
INSERT INTO student_profiles (user_id, student_number, program_id, curriculum_id, year_level, enrollment_status, student_classification, is_graduating, total_units_earned, cumulative_gpa, financial_clearance, departmental_clearance)
VALUES
(6, '2026-IT-0001',
 (SELECT id FROM programs WHERE code = 'BSIT' LIMIT 1),
 (SELECT id FROM curricula WHERE code = 'BSIT-2026' LIMIT 1),
 2, 'REGULAR', 'CONTINUING', FALSE, 26.00, 1.75, 'CLEARED', 'CLEARED')
ON DUPLICATE KEY UPDATE program_id = VALUES(program_id),
                        curriculum_id = VALUES(curriculum_id),
                        year_level = VALUES(year_level),
                        enrollment_status = VALUES(enrollment_status),
                        student_classification = VALUES(student_classification),
                        is_graduating = VALUES(is_graduating),
                        total_units_earned = VALUES(total_units_earned),
                        cumulative_gpa = VALUES(cumulative_gpa),
                        financial_clearance = VALUES(financial_clearance),
                        departmental_clearance = VALUES(departmental_clearance);

-- -----------------------------------------------------------------------------
-- 7. Student Historical Grades (Prerequisite Fulfillment Records)
-- -----------------------------------------------------------------------------
INSERT INTO student_course_grades (student_id, course_id, term_id, numerical_grade, completion_status, is_credited)
VALUES
((SELECT id FROM student_profiles WHERE student_number = '2026-IT-0001' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-USELF' LIMIT 1), NULL, 1.50, 'PASSED', TRUE),
((SELECT id FROM student_profiles WHERE student_number = '2026-IT-0001' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-RPH' LIMIT 1), NULL, 1.75, 'PASSED', TRUE),
((SELECT id FROM student_profiles WHERE student_number = '2026-IT-0001' LIMIT 1), (SELECT id FROM courses WHERE code = 'GE-PC' LIMIT 1), NULL, 1.50, 'PASSED', TRUE),
((SELECT id FROM student_profiles WHERE student_number = '2026-IT-0001' LIMIT 1), (SELECT id FROM courses WHERE code = 'PE-1' LIMIT 1), NULL, 1.25, 'PASSED', TRUE),
((SELECT id FROM student_profiles WHERE student_number = '2026-IT-0001' LIMIT 1), (SELECT id FROM courses WHERE code = 'NSTP-1' LIMIT 1), NULL, 1.25, 'PASSED', TRUE),
((SELECT id FROM student_profiles WHERE student_number = '2026-IT-0001' LIMIT 1), (SELECT id FROM courses WHERE code = 'IT-101' LIMIT 1), NULL, 1.50, 'PASSED', TRUE)
ON DUPLICATE KEY UPDATE numerical_grade = VALUES(numerical_grade),
                        completion_status = VALUES(completion_status),
                        is_credited = VALUES(is_credited);

-- -----------------------------------------------------------------------------
-- 8. Sample Refresh & Reset Tokens for Authentication Lifecycle Testing
-- -----------------------------------------------------------------------------
INSERT INTO refresh_tokens (user_id, token, expiry_date, revoked, created_at)
VALUES
(1, 'sample-active-admin-refresh-token-hash-xyz123', DATE_ADD(NOW(), INTERVAL 7 DAY), FALSE, NOW()),
(4, 'sample-active-faculty-refresh-token-hash-abc456', DATE_ADD(NOW(), INTERVAL 7 DAY), FALSE, NOW()),
(4, 'sample-revoked-token-for-theft-detection-789', DATE_ADD(NOW(), INTERVAL 7 DAY), TRUE, NOW()),
(6, 'sample-expired-token-for-student-john-000', DATE_SUB(NOW(), INTERVAL 1 DAY), FALSE, DATE_SUB(NOW(), INTERVAL 8 DAY))
ON DUPLICATE KEY UPDATE revoked = VALUES(revoked);

INSERT INTO password_reset_tokens (user_id, token, expiry_date, created_at)
VALUES
(6, SHA2('student-john-reset-token-sample', 256), DATE_ADD(NOW(), INTERVAL 30 MINUTE), NOW()),
(4, SHA2('faculty-alice-expired-reset-token', 256), DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 40 MINUTE))
ON DUPLICATE KEY UPDATE expiry_date = VALUES(expiry_date);

SET FOREIGN_KEY_CHECKS = 1;
