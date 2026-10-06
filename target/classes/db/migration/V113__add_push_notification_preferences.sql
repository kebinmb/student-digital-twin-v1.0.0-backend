-- V113__add_push_notification_preferences.sql
-- Subsystem: Granular Notification Preference Matrix for Web Push Subscriptions

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE web_push_subscriptions
ADD COLUMN notify_grades BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN notify_clearance BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN notify_honors BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN notify_attendance BOOLEAN NOT NULL DEFAULT TRUE;

SET FOREIGN_KEY_CHECKS = 1;
