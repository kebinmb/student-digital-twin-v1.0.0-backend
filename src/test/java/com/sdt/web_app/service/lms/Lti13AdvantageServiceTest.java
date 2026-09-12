package com.sdt.web_app.service.lms;

import com.sdt.web_app.dto.lms.LmsDtos.*;
import com.sdt.web_app.entities.lms.LtiDeployment;
import com.sdt.web_app.repositories.lms.LtiDeploymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Lti13AdvantageServiceTest {

    @Mock private LtiDeploymentRepository deploymentRepository;

    @InjectMocks
    private Lti13AdvantageService ltiService;

    private LtiDeployment mockDeployment;

    @BeforeEach
    void setUp() {
        mockDeployment = LtiDeployment.builder()
                .id(1L)
                .platformName("CANVAS")
                .clientId("canvas-client-123")
                .deploymentId("dep-001")
                .oidcAuthUrl("https://canvas.instructure.com/api/lti/authorize_redirect")
                .accessTokenUrl("https://canvas.instructure.com/login/oauth2/token")
                .jwksUrl("https://canvas.instructure.com/api/lti/security/jwks")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Create LTI deployment successfully")
    void createDeployment_Success() {
        when(deploymentRepository.save(any(LtiDeployment.class))).thenReturn(mockDeployment);

        LtiDeploymentRequest req = new LtiDeploymentRequest(
                "CANVAS", "canvas-client-123", "dep-001",
                "https://canvas.instructure.com/api/lti/authorize_redirect",
                "https://canvas.instructure.com/login/oauth2/token",
                "https://canvas.instructure.com/api/lti/security/jwks",
                true
        );

        LtiDeploymentResponse res = ltiService.createDeployment(req);

        assertThat(res).isNotNull();
        assertThat(res.platformName()).isEqualTo("CANVAS");
        assertThat(res.clientId()).isEqualTo("canvas-client-123");
    }

    @Test
    @DisplayName("Initiate OIDC handshake constructs valid authorization URL")
    void initiateOidcHandshake_Success() {
        when(deploymentRepository.findByClientIdAndDeploymentId("canvas-client-123", "dep-001"))
                .thenReturn(Optional.of(mockDeployment));

        String authUrl = ltiService.initiateOidcHandshake("canvas-client-123", "dep-001", "https://sdt.edu.ph/api/v1/lti/launch");

        assertThat(authUrl).contains("https://canvas.instructure.com/api/lti/authorize_redirect");
        assertThat(authUrl).contains("client_id=canvas-client-123");
        assertThat(authUrl).contains("response_type=id_token");
    }
}
