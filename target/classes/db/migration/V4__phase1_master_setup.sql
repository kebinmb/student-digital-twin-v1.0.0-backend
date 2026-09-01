-- V4__phase1_master_setup.sql
-- Phase 1 Foundation Layer Master Setup (RBAC Permissions, Institutional Structure & Fee Catalogs)

-- 1. Granular Permissions & Role-Permission Mappings
CREATE TABLE IF NOT EXISTS permissions
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permissions
(
    role          VARCHAR(30) NOT NULL,
    permission_id BIGINT      NOT NULL,
    PRIMARY KEY (role, permission_id),
    CONSTRAINT fk_role_permissions_perm FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 2. Institutional Master Data & Academic Structures
CREATE TABLE IF NOT EXISTS campuses
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    code      VARCHAR(20)  NOT NULL UNIQUE,
    name      VARCHAR(100) NOT NULL,
    address   TEXT,
    is_main   BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS departments
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    campus_id    BIGINT       NOT NULL,
    code         VARCHAR(20)  NOT NULL UNIQUE,
    name         VARCHAR(100) NOT NULL,
    dean_user_id BIGINT,
    is_active    BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_departments_campus FOREIGN KEY (campus_id) REFERENCES campuses (id) ON DELETE CASCADE,
    CONSTRAINT fk_departments_dean FOREIGN KEY (dean_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS academic_years
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(20) NOT NULL UNIQUE, -- e.g. '2026-2027'
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    is_current BOOLEAN DEFAULT FALSE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS terms
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year_id BIGINT      NOT NULL,
    term_name        VARCHAR(30) NOT NULL, -- e.g. '1st Semester', '2nd Semester', 'Summer'
    enrollment_open  BOOLEAN DEFAULT FALSE,
    grading_open     BOOLEAN DEFAULT FALSE,
    add_drop_open    BOOLEAN DEFAULT FALSE,
    is_active        BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_terms_ay FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS grading_scales
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    numeric_grade    DECIMAL(3, 2) NOT NULL UNIQUE, -- e.g. 1.00, 1.25, 1.50, 3.00, 5.00
    percentage_min   DECIMAL(5, 2) NOT NULL,
    percentage_max   DECIMAL(5, 2) NOT NULL,
    transmuted_grade VARCHAR(10),
    remarks          VARCHAR(50)   NOT NULL,        -- 'EXCELLENT', 'PASSED', 'FAILED'
    is_passing       BOOLEAN DEFAULT TRUE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 3. Financial Master Data & Fee Catalogs (CHED / RA 10931 Alignment)
CREATE TABLE IF NOT EXISTS fee_categories
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30)  NOT NULL UNIQUE, -- 'TUITION', 'MISC_MANDATORY', 'OTHER', 'LABORATORY'
    name VARCHAR(100) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fee_catalog
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id        BIGINT         NOT NULL,
    code               VARCHAR(30)    NOT NULL UNIQUE,
    name               VARCHAR(100)   NOT NULL,
    default_amount     DECIMAL(10, 2) NOT NULL,
    is_per_unit        BOOLEAN DEFAULT FALSE,
    is_ched_sanctioned BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_fee_catalog_category FOREIGN KEY (category_id) REFERENCES fee_categories (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS scholarship_discounts
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    code                VARCHAR(30)  NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    type                VARCHAR(30)  NOT NULL, -- 'UNIFAST_FHE', 'CHED_TDP', 'ACADEMIC_FULL', 'DISCOUNT_FIXED'
    discount_percentage DECIMAL(5, 2)  DEFAULT 0.00,
    fixed_amount        DECIMAL(10, 2) DEFAULT 0.00,
    applies_to_tuition  BOOLEAN        DEFAULT TRUE,
    applies_to_misc     BOOLEAN        DEFAULT TRUE,
    funding_source      VARCHAR(100)           -- 'CHED UniFAST', 'Institutional', 'LGU'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_term_templates
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(50)   NOT NULL,
    downpayment_pct DECIMAL(5, 2) NOT NULL,
    prelim_pct      DECIMAL(5, 2) NOT NULL,
    midterm_pct     DECIMAL(5, 2) NOT NULL,
    final_pct       DECIMAL(5, 2) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;