-- V109__create_campus_notices.sql
-- Subsystem: Campus Notices & Official Institutional Advisories

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS campus_notices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(60) NOT NULL,
    content TEXT NOT NULL,
    audience VARCHAR(30) NOT NULL DEFAULT 'ALL',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    author_id BIGINT NULL,
    author_role VARCHAR(50) NULL,
    is_pinned TINYINT(1) NOT NULL DEFAULT 0,
    is_published TINYINT(1) NOT NULL DEFAULT 1,
    publish_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_notice_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_notices_published ON campus_notices (is_published, publish_at);
CREATE INDEX idx_notices_audience ON campus_notices (audience);
CREATE INDEX idx_notices_category ON campus_notices (category);

CREATE TABLE IF NOT EXISTS notice_user_acknowledgments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notice_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    acknowledged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ack_notice FOREIGN KEY (notice_id) REFERENCES campus_notices (id) ON DELETE CASCADE,
    CONSTRAINT fk_ack_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_notice_user_ack UNIQUE (notice_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_ack_user ON notice_user_acknowledgments (user_id);

-- Seed initial institutional notices
INSERT INTO campus_notices (id, title, category, content, audience, priority, author_role, is_published, publish_at)
VALUES 
(1, 'Midterm Examination Schedule AY 2026-2027 Released', 'Registrar', 'Official midterm examination timetable for 1st Semester AY 2026-2027 has been finalized. Room allocations and schedules are viewable in the portal.', 'ALL', 'IMPORTANT', 'REGISTRAR', 1, CURRENT_TIMESTAMP),
(2, 'Online Encoding of Student Clearance Now Open', 'Student Affairs', 'Students may now settle departmental, library, and laboratory clearances online via the clearance module before the end of the term.', 'STUDENT', 'NORMAL', 'ADMIN', 1, CURRENT_TIMESTAMP),
(3, 'CHMSU ICT Helpdesk Maintenance on Saturday 10 PM', 'ICT Office', 'Scheduled server maintenance and infrastructure database optimization will occur this Saturday at 10:00 PM for approximately 2 hours.', 'ALL', 'NORMAL', 'ADMIN', 1, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE title = VALUES(title);

SET FOREIGN_KEY_CHECKS = 1;
