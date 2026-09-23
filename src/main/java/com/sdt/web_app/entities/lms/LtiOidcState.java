package com.sdt.web_app.entities.lms;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "lti_oidc_states")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LtiOidcState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String state;

    @Column(nullable = false, length = 128)
    private String nonce;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "deployment_id", nullable = false, length = 100)
    private String deploymentId;

    @Column(name = "target_link_uri", nullable = false, length = 512)
    private String targetLinkUri;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
