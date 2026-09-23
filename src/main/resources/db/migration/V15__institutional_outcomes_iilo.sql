-- V15__institutional_outcomes_iilo.sql
-- Create Institutional Intended Learning Outcomes (IILO) schema for 3-tier OBE hierarchy

CREATE TABLE IF NOT EXISTS institutional_outcomes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    statement TEXT NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_iilo_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pilo_iilo_mappings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pilo_id BIGINT NOT NULL,
    iilo_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pilo_iilo UNIQUE (pilo_id, iilo_id),
    CONSTRAINT fk_pilo_iilo_pilo FOREIGN KEY (pilo_id) REFERENCES program_outcomes(id) ON DELETE CASCADE,
    CONSTRAINT fk_pilo_iilo_iilo FOREIGN KEY (iilo_id) REFERENCES institutional_outcomes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
