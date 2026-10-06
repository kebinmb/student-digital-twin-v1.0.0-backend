CREATE TABLE IF NOT EXISTS certificate_revocations (
                                                       id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                                       certificate_id VARCHAR(120) NOT NULL UNIQUE,
    revoked_by_user_id BIGINT NULL,
    revocation_reason VARCHAR(500) NOT NULL,
    revoked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    reinstated_at TIMESTAMP NULL DEFAULT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    CONSTRAINT fk_cert_revocations_user
    FOREIGN KEY (revoked_by_user_id) REFERENCES users(id)
    ON DELETE SET NULL
    );

CREATE INDEX idx_cert_revocations_is_active ON certificate_revocations(is_active);