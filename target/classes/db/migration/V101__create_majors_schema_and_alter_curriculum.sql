-- V101__create_majors_schema_and_alter_curriculum.sql
-- Subsystem: Major Entity, Schema Extension & Dynamic Curriculum Linking

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS majors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    program_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_majors_program FOREIGN KEY (program_id) REFERENCES programs(id) ON DELETE CASCADE,
    CONSTRAINT uq_program_major_code UNIQUE (program_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE majors CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE INDEX idx_majors_program_id ON majors(program_id);
CREATE INDEX idx_majors_code ON majors(code);

-- Check and add major_id column to curricula table
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'curricula' 
      AND COLUMN_NAME = 'major_id'
);

SET @sql_stmt = IF(@col_exists = 0,
    'ALTER TABLE curricula ADD COLUMN major_id BIGINT NULL AFTER program_id, ADD CONSTRAINT fk_curricula_major FOREIGN KEY (major_id) REFERENCES majors(id) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add index on major_id
SET @idx_exists = (
    SELECT COUNT(*) 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'curricula' 
      AND INDEX_NAME = 'idx_curricula_major_id'
);

SET @sql_idx = IF(@idx_exists = 0,
    'CREATE INDEX idx_curricula_major_id ON curricula(major_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure credit_units exists on curriculum_courses for fast term unit summation
SET @cu_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'curriculum_courses' 
      AND COLUMN_NAME = 'credit_units'
);

SET @sql_cu = IF(@cu_exists = 0,
    'ALTER TABLE curriculum_courses ADD COLUMN credit_units DECIMAL(4, 2) NOT NULL DEFAULT 3.00 AFTER course_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql_cu;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Backfill credit_units from courses table
UPDATE curriculum_courses cc
JOIN courses c ON cc.course_id = c.id
SET cc.credit_units = c.credit_units;

-- Backward compatibility view for any tooling/queries referencing 'curriculums'
CREATE OR REPLACE VIEW curriculums AS SELECT * FROM curricula;

SET FOREIGN_KEY_CHECKS = 1;
