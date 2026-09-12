-- V8__lms_lti_interoperability.sql
-- Subsystem: LTI 1.3 Advantage Deployments, LMS Mappings & Roster Sync

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS lti_deployments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform_name VARCHAR(100) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    deployment_id VARCHAR(255) NOT NULL,
    oidc_auth_url VARCHAR(255) NOT NULL,
    access_token_url VARCHAR(255) NOT NULL,
    jwks_url VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS lti_user_mappings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lti_deployment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    sub_claim VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lum_deployment FOREIGN KEY (lti_deployment_id) REFERENCES lti_deployments (id) ON DELETE CASCADE,
    CONSTRAINT fk_lum_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_lum_deployment_sub UNIQUE (lti_deployment_id, sub_claim)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
