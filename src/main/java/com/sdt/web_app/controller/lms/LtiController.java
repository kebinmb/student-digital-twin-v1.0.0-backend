package com.sdt.web_app.controller.lms;

import com.sdt.web_app.dto.lms.LmsDtos.*;
import com.sdt.web_app.service.lms.Lti13AdvantageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lti")
@RequiredArgsConstructor
public class LtiController {

    private final Lti13AdvantageService ltiService;

    @PostMapping("/deployments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LtiDeploymentResponse> createDeployment(@Valid @RequestBody LtiDeploymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ltiService.createDeployment(request));
    }

    @GetMapping("/deployments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LtiDeploymentResponse>> getDeployments() {
        return ResponseEntity.ok(ltiService.getDeployments());
    }

    @GetMapping("/login")
    public ResponseEntity<String> initiateOidcLogin(
            @RequestParam("client_id") String clientId,
            @RequestParam("deployment_id") String deploymentId,
            @RequestParam("target_link_uri") String targetLinkUri) {
        return ResponseEntity.ok(ltiService.initiateOidcHandshake(clientId, deploymentId, targetLinkUri));
    }

    @PostMapping("/launch")
    public ResponseEntity<LtiLaunchResponse> validateLaunchToken(@Valid @RequestBody LtiLaunchRequest request) {
        return ResponseEntity.ok(ltiService.validateLaunchToken(request));
    }
}
