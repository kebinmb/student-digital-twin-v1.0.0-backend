-- V14__lti_state_and_ags.sql
-- LTI 1.3 Advantage OIDC States & Assignment and Grade Services (AGS 2.0) Schema

CREATE TABLE IF NOT EXISTS lti_oidc_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    state VARCHAR(128) NOT NULL UNIQUE,
    nonce VARCHAR(128) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    deployment_id VARCHAR(100) NOT NULL,
    target_link_uri VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_lti_state (state),
    INDEX idx_lti_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lti_lineitems (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    deployment_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    lineitem_url VARCHAR(512) NOT NULL,
    label VARCHAR(255) NOT NULL,
    maximum_score DECIMAL(5,2) NOT NULL DEFAULT 100.00,
    resource_id VARCHAR(100),
    tag VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lti_lineitem_deployment FOREIGN KEY (deployment_id) REFERENCES lti_deployments(id) ON DELETE CASCADE,
    CONSTRAINT fk_lti_lineitem_section FOREIGN KEY (section_id) REFERENCES class_sections(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
