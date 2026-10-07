package com.sdt.web_app.controller.analytics;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.analytics.AnalyticsDtos.StudentTelemetryAdminSummaryDto;
import com.sdt.web_app.dto.analytics.AnalyticsDtos.TelemetryKpiSummaryDto;
import com.sdt.web_app.service.analytics.DigitalTwinRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/telemetry")
@RequiredArgsConstructor
public class AdminTelemetryController {

    private final DigitalTwinRiskService riskService;

    @Auditable(action = "READ_ADMIN_TELEMETRY", entityName = "StudentTelemetry")
    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GUIDANCE', 'STUDENT_AFFAIRS')")
    public ResponseEntity<Page<StudentTelemetryAdminSummaryDto>> getAdminStudentTelemetry(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "searchQuery", required = false) String searchQuery,
            @RequestParam(name = "riskLevel", required = false) String riskLevel,
            @RequestParam(name = "interventionStatus", required = false) String interventionStatus) {
        
        Page<StudentTelemetryAdminSummaryDto> telemetryPage = riskService.getAdminStudentTelemetry(
                page, size, searchQuery, riskLevel, interventionStatus);
        return ResponseEntity.ok(telemetryPage);
    }

    @Auditable(action = "READ_ADMIN_TELEMETRY_KPI", entityName = "StudentTelemetry")
    @GetMapping("/kpi")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'GUIDANCE', 'STUDENT_AFFAIRS')")
    public ResponseEntity<TelemetryKpiSummaryDto> getAdminTelemetryKpi(
            @RequestParam(name = "searchQuery", required = false) String searchQuery,
            @RequestParam(name = "riskLevel", required = false) String riskLevel,
            @RequestParam(name = "interventionStatus", required = false) String interventionStatus) {
        TelemetryKpiSummaryDto kpi = riskService.getAdminTelemetryKpi(searchQuery, riskLevel, interventionStatus);
        return ResponseEntity.ok(kpi);
    }
}
