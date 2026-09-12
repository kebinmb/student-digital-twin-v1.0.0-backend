package com.sdt.web_app.entities.lms;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "lti_deployments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LtiDeployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "platform_name", nullable = false, length = 100)
    private String platformName;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "deployment_id", nullable = false)
    private String deploymentId;

    @Column(name = "oidc_auth_url", nullable = false)
    private String oidcAuthUrl;

    @Column(name = "access_token_url", nullable = false)
    private String accessTokenUrl;

    @Column(name = "jwks_url", nullable = false)
    private String jwksUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateConfig(String platformName, String clientId, String deploymentId, String oidcAuthUrl, String accessTokenUrl, String jwksUrl, Boolean active) {
        if (platformName != null) this.platformName = platformName;
        if (clientId != null) this.clientId = clientId;
        if (deploymentId != null) this.deploymentId = deploymentId;
        if (oidcAuthUrl != null) this.oidcAuthUrl = oidcAuthUrl;
        if (accessTokenUrl != null) this.accessTokenUrl = accessTokenUrl;
        if (jwksUrl != null) this.jwksUrl = jwksUrl;
        if (active != null) this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LtiDeployment that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
