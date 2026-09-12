-- V1__system_master_data_and_rbac.sql
-- Subsystem: Core Identity, Security, User Master Data, RBAC Roles, Tokens & System Audit Trail
-- Standard Compliance: MySQL 8.0 / InnoDB / utf8mb4

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    college_id BIGINT NULL,
    program_id BIGINT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_users_college ON users (college_id);
CREATE INDEX idx_users_program ON users (program_id);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens (token);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);

CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    username VARCHAR(50) NULL,
    action VARCHAR(50) NOT NULL,
    entity_name VARCHAR(100) NULL,
    entity_id VARCHAR(100) NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(255) NULL,
    details TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT NULL,
    execution_time_ms BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_name);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);

-- Seed System Reference Permissions
INSERT INTO permissions (id, name, description)
VALUES (1, 'permissions:system:manage', 'Manage system configurations and security policies'),
       (2, 'academic:hierarchy:manage', 'Manage campus, department, and degree program master records'),
       (3, 'curriculum:author:approve', 'Design, update, and approve academic curricula'),
       (4, 'student:profile:view', 'View core student biographical and academic profile'),
       (5, 'student:equity:manage', 'Access and update statutory equity and vulnerability data'),
       (6, 'enrollment:advising:process', 'Evaluate prerequisites and process student enrollment'),
       (7, 'assessment:unifast:bill', 'Generate CHED-UniFAST Free Higher Education (FHE) billing statements'),
       (8, 'grading:encode:submit', 'Encode and submit mid-term and final grades'),
       (9, 'grading:registrar:lock', 'Verify, transmute, and lock academic grades'),
       (10, 'records:tor:issue', 'Generate official Transcript of Records and honorable dismissals'),
       (11, 'ched:hemis:export', 'Extract Form E-1 through E-5 compliance reporting data')
AS new_perm ON DUPLICATE KEY UPDATE name = new_perm.name, description = new_perm.description;

-- Argon2id Hash '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE' represents 'Password123!'
INSERT INTO users (id, username, college_id, program_id, email, password, enabled)
VALUES (1, 'admin', NULL, NULL, 'admin@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
       (2, 'registrar', NULL, NULL, 'registrar@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
       (3, 'dean_cit', NULL, NULL, 'dean.cit@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
       (4, 'chair_it', NULL, NULL, 'chair.it@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
       (5, 'prof_juan', NULL, NULL, 'juan.delacruz@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
       (6, 'student_maria', NULL, NULL, 'maria.clara@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
       (7, 'cashier_jose', NULL, NULL, 'cashier.jose@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE),
       (8, 'accountant_ana', NULL, NULL, 'accountant.ana@chmsu.edu.ph', '$argon2id$v=19$m=16384,t=2,p=1$/abeTiesW90uK3Z8i5qJ/Q$ZBbpX5UvxC65aWXFD2E+/Evjt9cMFcDB/U8iqbvb3iE', TRUE)
AS new_user ON DUPLICATE KEY UPDATE email = new_user.email, password = new_user.password;

INSERT INTO user_roles (user_id, role)
VALUES (1, 'ADMIN'),
       (2, 'REGISTRAR'),
       (3, 'DEAN'),
       (4, 'CHAIRPERSON'),
       (5, 'FACULTY'),
       (6, 'STUDENT'),
       (7, 'CASHIER'),
       (8, 'ADMIN')
AS new_role ON DUPLICATE KEY UPDATE role = new_role.role;

SET FOREIGN_KEY_CHECKS = 1;
