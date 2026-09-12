package com.sdt.web_app.controller.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.*;
import com.sdt.web_app.service.analytics.DigitalTwinRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics/digital-twin")
@RequiredArgsConstructor
public class DigitalTwinAnalyticsController {

    private final DigitalTwinRiskService riskService;

    @GetMapping("/risk/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'STUDENT')")
    public ResponseEntity<DigitalTwinRiskProfileDto> getStudentRiskProfile(@PathVariable("studentId") Long studentId) {
        return ResponseEntity.ok(riskService.evaluateStudentRiskProfile(studentId));
    }

    @GetMapping("/early-warning/radar")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'GUIDANCE')")
    public ResponseEntity<List<EarlyWarningRadarItemDto>> getEarlyWarningRadar() {
        return ResponseEntity.ok(riskService.getEarlyWarningRadar());
    }
}
