-- V112__create_web_push_subscriptions.sql
-- Subsystem: W3C Push API / Web Push Subscription Registry for Real-Time Background Notification Dispatch

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS web_push_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    endpoint VARCHAR(1024) NOT NULL,
    p256dh_key VARCHAR(255) NOT NULL,
    auth_key VARCHAR(255) NOT NULL,
    user_agent VARCHAR(512) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_push_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_web_push_user ON web_push_subscriptions (user_id, is_active);
CREATE INDEX idx_web_push_endpoint ON web_push_subscriptions (endpoint(255));

SET FOREIGN_KEY_CHECKS = 1;
