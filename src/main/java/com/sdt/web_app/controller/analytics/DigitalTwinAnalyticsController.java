package com.sdt.web_app.controller.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.*;
import com.sdt.web_app.dto.enrollment.EnrollmentDtos.StudentProfileResponse;
import com.sdt.web_app.service.analytics.DigitalTwinRiskService;
import com.sdt.web_app.service.enrollment.StudentService;
import com.sdt.web_app.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics/digital-twin")
@RequiredArgsConstructor
public class DigitalTwinAnalyticsController {

    private final DigitalTwinRiskService riskService;
    private final StudentService studentService;
    private final SecurityUtils securityUtils;

    @GetMapping("/risk/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<DigitalTwinRiskProfileDto> getCurrentStudentRiskProfile(Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        if (userId == null) {
            throw new IllegalStateException("Cannot resolve authenticated student user identity.");
        }
        StudentProfileResponse student = studentService.getStudentByUserId(userId);
        return ResponseEntity.ok(riskService.evaluateStudentRiskProfile(student.id()));
    }

    @GetMapping("/risk/{studentId:[0-9]+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'STUDENT')")
    public ResponseEntity<DigitalTwinRiskProfileDto> getStudentRiskProfile(@PathVariable("studentId") Long studentId) {
        return ResponseEntity.ok(riskService.evaluateStudentRiskProfile(studentId));
    }

    @GetMapping("/early-warning/radar")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'GUIDANCE')")
    public ResponseEntity<List<EarlyWarningRadarItemDto>> getEarlyWarningRadar() {
        return ResponseEntity.ok(riskService.getEarlyWarningRadar());
    }

    @GetMapping("/early-warning/slice")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'GUIDANCE')")
    public ResponseEntity<com.sdt.web_app.dto.common.SliceResponse<DigitalTwinRiskProfileDto>> getEarlyWarningRadarSlice(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "DESC") String sortDir) {
        return ResponseEntity.ok(riskService.getEarlyWarningRadarSlice(page, size, sortBy, sortDir));
    }
}
