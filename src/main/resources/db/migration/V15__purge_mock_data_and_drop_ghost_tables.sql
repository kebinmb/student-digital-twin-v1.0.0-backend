-- V15__purge_mock_data_and_drop_ghost_tables.sql
-- Subsystem: Schema Synchronization, Ghost Table Elimination & Pristine State Initialization
-- Purpose: 
-- 1. Drop ghost table 'role_permissions' which has no JPA Entity or repository mapping.
-- 2. Drop orphan column 'term_name' from 'terms' table which has no matching property in Term.java.
-- 3. Purge all test/mock/seed data across all institutional domain, transactional, and token tables.
-- 4. Retain strictly ONE user: root administrator ('admin' / 'Password123!') with ROLE_ADMIN and NULL scoping.

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. Eliminate Ghost Tables & Orphan Elements
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS role_permissions;

ALTER TABLE terms DROP COLUMN term_name;

-- -----------------------------------------------------------------------------
-- 2. Purge Transactional, Operational & Student Domain Records
-- -----------------------------------------------------------------------------
TRUNCATE TABLE student_assessment_scores;
TRUNCATE TABLE class_record_items;
TRUNCATE TABLE section_grading_categories;
TRUNCATE TABLE section_grading_configs;

TRUNCATE TABLE enrollment_course_items;
TRUNCATE TABLE student_enrollments;
TRUNCATE TABLE course_equivalencies;
TRUNCATE TABLE student_course_grades;
TRUNCATE TABLE class_schedules;
TRUNCATE TABLE class_sections;
TRUNCATE TABLE faculty_workloads;

TRUNCATE TABLE student_profiles;
TRUNCATE TABLE faculty_profiles;

-- -----------------------------------------------------------------------------
-- 3. Purge OBE, Curricula & Course Catalogs
-- -----------------------------------------------------------------------------
TRUNCATE TABLE cilo_pilo_mappings;
TRUNCATE TABLE course_outcomes;
TRUNCATE TABLE program_outcomes;
TRUNCATE TABLE course_prerequisites;
TRUNCATE TABLE curriculum_courses;
TRUNCATE TABLE curricula;
TRUNCATE TABLE courses;

-- -----------------------------------------------------------------------------
-- 4. Purge Academic Structure & Physical Facilities
-- -----------------------------------------------------------------------------
TRUNCATE TABLE terms;
TRUNCATE TABLE academic_years;
TRUNCATE TABLE rooms;
TRUNCATE TABLE programs;
TRUNCATE TABLE departments;
TRUNCATE TABLE campuses;

-- -----------------------------------------------------------------------------
-- 5. Purge Fee, Finance & Grading Reference Data
-- -----------------------------------------------------------------------------
TRUNCATE TABLE fee_catalog;
TRUNCATE TABLE fee_categories;
TRUNCATE TABLE scholarship_discounts;
TRUNCATE TABLE payment_term_templates;
TRUNCATE TABLE grading_scales;
TRUNCATE TABLE permissions;

-- -----------------------------------------------------------------------------
-- 6. Purge Audit Logs & Authentication Tokens
-- -----------------------------------------------------------------------------
TRUNCATE TABLE refresh_tokens;
TRUNCATE TABLE password_reset_tokens;
TRUNCATE TABLE audit_logs;

-- -----------------------------------------------------------------------------
-- 7. Reset Users Table to Sole Root Administrator
-- -----------------------------------------------------------------------------
TRUNCATE TABLE user_roles;
TRUNCATE TABLE users;

INSERT INTO users (id, username, email, password, enabled, college_id, program_id, created_at)
VALUES (
    1,
    'admin',
    'admin@example.com',
    '$argon2id$v=19$m=16384,t=2,p=1$Ed3PWKV7XMiHtZvKD7Wu1w$iNk1epo/wYCSevNZgCwCGVXGwlx0Lq42vYOuQ0Ki7kg',
    TRUE,
    NULL,
    NULL,
    NOW()
);

INSERT INTO user_roles (user_id, role)
VALUES (1, 'ADMIN');

ALTER TABLE users AUTO_INCREMENT = 2;

SET FOREIGN_KEY_CHECKS = 1;
