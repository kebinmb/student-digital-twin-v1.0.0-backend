-- V2__institutional_master_setup.sql
-- Subsystem: Institutional Master Setup (Campuses, Colleges, Departments, Academic Years, Terms, Rooms)

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS campuses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    ched_institutional_code VARCHAR(20) NULL,
    name VARCHAR(100) NOT NULL,
    address TEXT NULL,
    region VARCHAR(50) NOT NULL DEFAULT 'REGION VI',
    contact_number VARCHAR(30) NULL,
    email VARCHAR(100) NULL,
    is_main BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campus_id BIGINT NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'COLLEGE',
    parent_department_id BIGINT NULL,
    dean_user_id BIGINT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_campus_dept_code UNIQUE (campus_id, code),
    CONSTRAINT fk_departments_campus FOREIGN KEY (campus_id) REFERENCES campuses (id) ON DELETE CASCADE,
    CONSTRAINT fk_departments_parent FOREIGN KEY (parent_department_id) REFERENCES departments (id) ON DELETE SET NULL,
    CONSTRAINT fk_departments_dean FOREIGN KEY (dean_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Add foreign key constraint back to users for college_id
ALTER TABLE users 
    ADD CONSTRAINT fk_users_college FOREIGN KEY (college_id) REFERENCES departments (id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS academic_years (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS terms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    term_type VARCHAR(20) NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    enrollment_open BOOLEAN NOT NULL DEFAULT FALSE,
    grading_open BOOLEAN NOT NULL DEFAULT FALSE,
    add_drop_open BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    max_hours_per_class DECIMAL(3, 1) NOT NULL DEFAULT 3.0,
    CONSTRAINT uq_ay_term UNIQUE (academic_year_id, term_type),
    CONSTRAINT fk_terms_ay FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campus_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    building VARCHAR(100) NOT NULL,
    floor INT NOT NULL DEFAULT 1,
    capacity INT NOT NULL DEFAULT 40,
    room_type VARCHAR(30) NOT NULL DEFAULT 'LECTURE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_campus_room_code UNIQUE (campus_id, code),
    CONSTRAINT fk_rooms_campus FOREIGN KEY (campus_id) REFERENCES campuses (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Seed Institutional Master Foundation
INSERT INTO campuses (id, code, ched_institutional_code, name, address, region, contact_number, email, is_main, is_active)
VALUES (1, 'TALISAY', '06001', 'Talisay Main Campus', 'Mabini St., Talisay City, Negros Occidental', 'REGION VI', '(034) 712-0000', 'talisay.main@chmsu.edu.ph', TRUE, TRUE),
       (2, 'FORTUNE_TOWNE', '06002', 'Fortune Towne Campus', 'Brgy. Estefania, Bacolod City', 'REGION VI', '(034) 434-1234', 'fortunetowne@chmsu.edu.ph', FALSE, TRUE),
       (3, 'BINALBAGAN', '06003', 'Binalbagan Campus', 'Brgy. Enclaro, Binalbagan, Negros Occidental', 'REGION VI', '(034) 388-5678', 'binalbagan@chmsu.edu.ph', FALSE, TRUE),
       (4, 'ALJJ', '06004', 'Alijis Campus', 'Brgy. Alijis, Bacolod City', 'REGION VI', '(034) 432-8899', 'alijis@chmsu.edu.ph', FALSE, TRUE)
AS new_campus ON DUPLICATE KEY UPDATE name = new_campus.name, is_main = new_campus.is_main;

INSERT INTO departments (id, campus_id, code, name, type, parent_department_id, dean_user_id, is_active)
VALUES (1, 1, 'CIT', 'College of Industrial Technology', 'COLLEGE', NULL, NULL, TRUE),
       (2, 1, 'COE', 'College of Engineering', 'COLLEGE', NULL, NULL, TRUE),
       (3, 1, 'COED', 'College of Education', 'COLLEGE', NULL, NULL, TRUE),
       (4, 2, 'CBMA', 'College of Business Management and Accountancy', 'COLLEGE', NULL, NULL, TRUE),
       (5, 1, 'DIT', 'Department of Information Technology', 'ACADEMIC_DEPT', 1, NULL, TRUE),
       (6, 1, 'DCPE', 'Department of Computer Engineering', 'ACADEMIC_DEPT', 2, NULL, TRUE)
AS new_dept ON DUPLICATE KEY UPDATE name = new_dept.name, type = new_dept.type;

INSERT INTO academic_years (id, code, start_date, end_date, is_current)
VALUES (1, 'AY-2023-2024', '2023-08-14', '2024-06-30', FALSE),
       (2, 'AY-2024-2025', '2024-08-12', '2025-06-27', TRUE),
       (3, 'AY-2025-2026', '2025-08-11', '2026-06-26', FALSE)
AS new_ay ON DUPLICATE KEY UPDATE is_current = new_ay.is_current;

INSERT INTO terms (id, academic_year_id, term_type, start_date, end_date, enrollment_open, grading_open, add_drop_open, is_active, max_hours_per_class)
VALUES (1, 1, 'FIRST_SEM', '2023-08-14', '2023-12-22', FALSE, FALSE, FALSE, FALSE, 3.0),
       (2, 1, 'SECOND_SEM', '2024-01-15', '2024-05-31', FALSE, FALSE, FALSE, FALSE, 3.0),
       (3, 2, 'FIRST_SEM', '2024-08-12', '2024-12-20', FALSE, FALSE, FALSE, FALSE, 3.0),
       (4, 2, 'SECOND_SEM', '2025-01-13', '2025-05-30', TRUE, TRUE, TRUE, TRUE, 3.0)
AS new_term ON DUPLICATE KEY UPDATE is_active = new_term.is_active, enrollment_open = new_term.enrollment_open;

INSERT INTO rooms (id, campus_id, code, name, building, floor, capacity, room_type, is_active)
VALUES (1, 1, 'CL-101', 'Computer Laboratory 101', 'Technology Building', 1, 40, 'COMPUTER_LAB', TRUE),
       (2, 1, 'CL-102', 'Computer Laboratory 102', 'Technology Building', 1, 40, 'COMPUTER_LAB', TRUE),
       (3, 1, 'LH-201', 'Lecture Hall 201', 'Academic Building A', 2, 60, 'LECTURE', TRUE),
       (4, 1, 'LR-305', 'Multimedia Room 305', 'Academic Building B', 3, 45, 'LECTURE', TRUE)
AS new_room ON DUPLICATE KEY UPDATE capacity = new_room.capacity;

-- Link bootstrap dean user to CIT college
UPDATE users SET college_id = 1 WHERE id IN (3, 4, 5, 6, 7, 8);

SET FOREIGN_KEY_CHECKS = 1;
