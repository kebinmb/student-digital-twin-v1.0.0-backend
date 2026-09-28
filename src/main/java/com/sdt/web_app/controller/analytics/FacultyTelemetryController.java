package com.sdt.web_app.controller.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.FacultySectionOptionDto;
import com.sdt.web_app.dto.analytics.AnalyticsDtos.StudentTelemetrySummaryDto;
import com.sdt.web_app.service.analytics.DigitalTwinRiskService;
import com.sdt.web_app.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/faculty/telemetry")
@RequiredArgsConstructor
public class FacultyTelemetryController {

    private final DigitalTwinRiskService riskService;
    private final SecurityUtils securityUtils;

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('FACULTY', 'CHAIRPERSON', 'DEAN', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<StudentTelemetrySummaryDto>> getFacultyStudentTelemetry(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "searchQuery", required = false) String searchQuery,
            @RequestParam(name = "riskLevel", required = false) String riskLevel,
            @RequestParam(name = "interventionStatus", required = false) String interventionStatus,
            @RequestParam(name = "sectionId", required = false) Long sectionId) {

        Long facultyUserId = securityUtils.resolveUserId(authentication);
        if (facultyUserId == null) {
            throw new IllegalStateException("Cannot resolve authenticated faculty user identity.");
        }

        Page<StudentTelemetrySummaryDto> telemetryPage = riskService.getFacultyStudentTelemetry(
                facultyUserId, page, size, searchQuery, riskLevel, interventionStatus, sectionId);
        return ResponseEntity.ok(telemetryPage);
    }

    @GetMapping("/sections")
    @PreAuthorize("hasAnyRole('FACULTY', 'CHAIRPERSON', 'DEAN', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<FacultySectionOptionDto>> getFacultyAssignedSections(Authentication authentication) {
        Long facultyUserId = securityUtils.resolveUserId(authentication);
        if (facultyUserId == null) {
            throw new IllegalStateException("Cannot resolve authenticated faculty user identity.");
        }
        return ResponseEntity.ok(riskService.getFacultyAssignedSections(facultyUserId));
    }
}
