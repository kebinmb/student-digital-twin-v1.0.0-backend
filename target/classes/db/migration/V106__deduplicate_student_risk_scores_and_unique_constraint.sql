-- V106__deduplicate_student_risk_scores_and_unique_constraint.sql
-- Subsystem: Student Risk Scores Deduplication & Unique Student Constraint

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Deduplicate existing rows keeping latest row per student
DELETE srs1 FROM student_risk_scores srs1
INNER JOIN student_risk_scores srs2 
WHERE srs1.student_profile_id = srs2.student_profile_id 
  AND srs1.id < srs2.id;

-- 2. Safely add unique constraint on student_profile_id
SET @idx_srs_uq_exists = (
    SELECT COUNT(*) 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'student_risk_scores' 
      AND INDEX_NAME = 'uq_srs_student'
);

SET @sql_srs_uq = IF(@idx_srs_uq_exists = 0,
    'ALTER TABLE student_risk_scores ADD CONSTRAINT uq_srs_student UNIQUE (student_profile_id)',
    'SELECT 1'
);
PREPARE stmt_uq FROM @sql_srs_uq;
EXECUTE stmt_uq;
DEALLOCATE PREPARE stmt_uq;

SET FOREIGN_KEY_CHECKS = 1;
