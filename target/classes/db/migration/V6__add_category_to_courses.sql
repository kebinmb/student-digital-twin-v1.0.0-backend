-- V6__add_category_to_courses.sql
-- Add curricular category column to courses table (CHED CMO No. 25, s. 2015)

ALTER TABLE courses ADD COLUMN category VARCHAR(30) DEFAULT 'PROFESSIONAL_MAJOR' NOT NULL;