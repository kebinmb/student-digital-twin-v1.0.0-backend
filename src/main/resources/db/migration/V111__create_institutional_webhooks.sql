-- V111__create_institutional_webhooks.sql
-- Subsystem: Outbound Institutional Webhook Dispatcher with Dead-Letter Retry Queue

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS institutional_webhooks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    secret_key VARCHAR(255) NOT NULL,
    subscribed_events VARCHAR(500) NOT NULL, -- Comma-separated: e.g. 'STUDENT_HONOR_AWARDED,TUITION_DISCOUNT_APPLIED,RISK_ALERT_TRIGGERED'
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_inst_webhook_active ON institutional_webhooks (is_active);

CREATE TABLE IF NOT EXISTS institutional_webhook_deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    webhook_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, DELIVERED, FAILED, DEAD_LETTER
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    last_attempt_at TIMESTAMP NULL,
    next_retry_at TIMESTAMP NULL,
    response_http_code INT NULL,
    response_body TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wh_delivery_webhook FOREIGN KEY (webhook_id) REFERENCES institutional_webhooks (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_wh_delivery_status_next ON institutional_webhook_deliveries (status, next_retry_at);
CREATE INDEX idx_wh_delivery_webhook ON institutional_webhook_deliveries (webhook_id);

SET FOREIGN_KEY_CHECKS = 1;
