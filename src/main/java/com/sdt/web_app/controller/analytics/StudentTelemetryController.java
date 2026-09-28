package com.sdt.web_app.controller.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.StudentSelfTelemetryDto;
import com.sdt.web_app.service.analytics.DigitalTwinRiskService;
import com.sdt.web_app.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student/telemetry")
@RequiredArgsConstructor
public class StudentTelemetryController {

    private final DigitalTwinRiskService riskService;
    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentSelfTelemetryDto> getStudentSelfTelemetry(Authentication authentication) {
        Long studentUserId = securityUtils.resolveUserId(authentication);
        if (studentUserId == null) {
            throw new IllegalStateException("Cannot resolve authenticated student user identity.");
        }
        return ResponseEntity.ok(riskService.getStudentSelfTelemetry(studentUserId));
    }

    @PostMapping("/interventions/{id}/acknowledge")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> acknowledgeIntervention(
            @PathVariable("id") Long interventionId,
            Authentication authentication) {
        Long studentUserId = securityUtils.resolveUserId(authentication);
        if (studentUserId == null) {
            throw new IllegalStateException("Cannot resolve authenticated student user identity.");
        }
        riskService.acknowledgeIntervention(interventionId, studentUserId);
        return ResponseEntity.noContent().build();
    }
}
