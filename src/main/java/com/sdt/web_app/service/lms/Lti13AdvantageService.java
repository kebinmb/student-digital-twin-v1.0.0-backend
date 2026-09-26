package com.sdt.web_app.service.lms;

import com.sdt.web_app.dto.lms.LmsDtos.*;
import com.sdt.web_app.entities.lms.LtiDeployment;
import com.sdt.web_app.repositories.lms.LtiDeploymentRepository;
import com.sdt.web_app.repositories.lms.LtiUserMappingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class Lti13AdvantageService {

    private final LtiDeploymentRepository deploymentRepository;
    private final LtiUserMappingRepository userMappingRepository;

    @Transactional
    public LtiDeploymentResponse createDeployment(LtiDeploymentRequest request) {
        LtiDeployment deployment = LtiDeployment.builder()
                .platformName(request.platformName())
                .clientId(request.clientId())
                .deploymentId(request.deploymentId())
                .oidcAuthUrl(request.oidcAuthUrl())
                .accessTokenUrl(request.accessTokenUrl())
                .jwksUrl(request.jwksUrl())
                .active(request.active() != null ? request.active() : true)
                .build();

        LtiDeployment saved = deploymentRepository.save(deployment);
        log.info("LTI 1.3 Advantage Deployment created for platform {}", saved.getPlatformName());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LtiDeploymentResponse> getDeployments() {
        return deploymentRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public String initiateOidcHandshake(String clientId, String deploymentId, String targetLinkUri) {
        LtiDeployment deployment = deploymentRepository.findByClientIdAndDeploymentId(clientId, deploymentId)
                .orElseThrow(() -> new EntityNotFoundException("LTI 1.3 deployment not registered for client " + clientId));

        if (!deployment.isActive()) {
            throw new IllegalStateException("LTI 1.3 deployment is inactive.");
        }

        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        return String.format("%s?response_type=id_token&response_mode=form_post&client_id=%s&scope=openid&state=%s&nonce=%s&prompt=none&redirect_uri=%s",
                deployment.getOidcAuthUrl(), clientId, state, nonce, targetLinkUri);
    }

    @Transactional
    public LtiLaunchResponse validateLaunchToken(LtiLaunchRequest request) {
        LtiDeployment deployment = deploymentRepository.findByClientIdAndDeploymentId(request.clientId(), request.deploymentId())
                .orElseThrow(() -> new EntityNotFoundException("LTI 1.3 deployment not found."));

        if (!deployment.isActive()) {
            throw new IllegalStateException("LTI 1.3 deployment is currently disabled.");
        }

        // Dynamically resolve user mapping if registered for this deployment
        java.util.Optional<com.sdt.web_app.entities.lms.LtiUserMapping> mappingOpt = userMappingRepository
                .findByDeploymentIdAndSubClaim(deployment.getId(), request.subClaim());

        String targetLink = "/portal/student";
        String username = "lti_user_" + request.subClaim().substring(0, Math.min(8, request.subClaim().length()));
        String role = "ROLE_STUDENT";

        if (mappingOpt.isPresent()) {
            com.sdt.web_app.entities.authentication.User mappedUser = mappingOpt.get().getUser();
            if (mappedUser != null) {
                username = mappedUser.getUsername();
                if (mappedUser.getRoles() != null && !mappedUser.getRoles().isEmpty()) {
                    role = "ROLE_" + mappedUser.getRoles().iterator().next().name();
                }
            }
        }

        log.info("LTI 1.3 Launch validated for sub claim {} on platform {}, resolved user {}", request.subClaim(), deployment.getPlatformName(), username);

        return new LtiLaunchResponse(
                targetLink,
                "lti-session-token-" + UUID.randomUUID(),
                username,
                role
        );
    }

    private LtiDeploymentResponse mapToResponse(LtiDeployment d) {
        return new LtiDeploymentResponse(
                d.getId(),
                d.getPlatformName(),
                d.getClientId(),
                d.getDeploymentId(),
                d.getOidcAuthUrl(),
                d.getAccessTokenUrl(),
                d.getJwksUrl(),
                d.isActive(),
                d.getCreatedAt()
        );
    }
}
