-- V3__add_audit_logs_schema.sql
-- Immutable Audit Logging Table for RA 10173 (Data Privacy Act of 2012) Compliance

CREATE TABLE IF NOT EXISTS audit_logs
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT,
    username          VARCHAR(50),
    action            VARCHAR(50)  NOT NULL,
    entity_name       VARCHAR(100),
    entity_id         VARCHAR(100),
    ip_address        VARCHAR(45)  NOT NULL,
    user_agent        VARCHAR(255),
    details           TEXT,
    status            VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
    error_message     TEXT,
    execution_time_ms BIGINT,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_name);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
