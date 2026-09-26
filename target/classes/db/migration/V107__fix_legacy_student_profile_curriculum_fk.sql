-- V107__fix_legacy_student_profile_curriculum_fk.sql
-- Fix legacy student_profiles whose curriculum_id references deprecated or non-existent curricula

SET FOREIGN_KEY_CHECKS = 0;

-- Update student_profiles pointing to legacy curriculum_id = 1 or invalid curricula to valid BSIT curriculum (11)
UPDATE student_profiles 
SET curriculum_id = 11 
WHERE curriculum_id = 1 
   OR curriculum_id NOT IN (SELECT id FROM (SELECT id FROM curricula) AS c_ids);

SET FOREIGN_KEY_CHECKS = 1;
