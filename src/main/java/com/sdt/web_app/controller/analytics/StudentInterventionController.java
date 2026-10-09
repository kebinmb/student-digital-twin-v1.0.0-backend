package com.sdt.web_app.controller.analytics;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.analytics.AnalyticsDtos.*;
import com.sdt.web_app.dto.common.SliceResponse;
import com.sdt.web_app.service.analytics.StudentInterventionService;
import com.sdt.web_app.entities.analytics.StudentIntervention;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.sdt.web_app.dto.analytics.AcknowledgeInterventionRequest;
import com.sdt.web_app.service.security.SecurityUtils;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics/interventions")
@RequiredArgsConstructor
public class StudentInterventionController {

    private final StudentInterventionService interventionService;
    private final SecurityUtils securityUtils;

    @Auditable(action = "READ_INTERVENTION_TYPES", entityName = "StudentIntervention")
    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getInterventionTypes() {
        return ResponseEntity.ok(
                java.util.Arrays.stream(StudentIntervention.InterventionType.values())
                        .map(Enum::name)
                        .toList()
        );
    }

    @Auditable(action = "DISPATCH_INTERVENTION", entityName = "StudentIntervention", entityId = "#result?.id()")
    @PostMapping("/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'GUIDANCE', 'STUDENT_AFFAIRS')")
    public ResponseEntity<StudentInterventionDto> dispatchIntervention(@Valid @RequestBody DispatchInterventionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(interventionService.dispatchIntervention(request));
    }

    @Auditable(action = "UPDATE_INTERVENTION_STATUS", entityName = "StudentIntervention", entityId = "#id")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'GUIDANCE', 'STUDENT_AFFAIRS')")
    public ResponseEntity<StudentInterventionDto> updateInterventionStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateInterventionStatusRequest request) {
        return ResponseEntity.ok(interventionService.updateInterventionStatus(id, request));
    }

    @Auditable(action = "ACKNOWLEDGE_INTERVENTION", entityName = "StudentIntervention", entityId = "#id")
    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT', 'GUIDANCE', 'STUDENT_AFFAIRS')")
    public ResponseEntity<StudentInterventionDto> acknowledgeIntervention(
            @PathVariable("id") Long id,
            Authentication authentication,
            @RequestBody(required = false) AcknowledgeInterventionRequest request) {
        boolean isStudent = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("STUDENT"));
        Long studentUserId = isStudent ? securityUtils.resolveUserId(authentication) : null;
        if (isStudent && studentUserId == null) {
            throw new IllegalStateException("Cannot resolve authenticated student user identity.");
        }
        return ResponseEntity.ok(interventionService.acknowledgeIntervention(id, studentUserId, request));
    }

    @Auditable(action = "READ_STUDENT_INTERVENTIONS", entityName = "StudentIntervention", entityId = "#studentId")
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUIDANCE', 'STUDENT_AFFAIRS') or @enrollmentSecurity.canAccessStudentAdvising(authentication, #studentId)")
    public ResponseEntity<List<StudentInterventionDto>> getStudentInterventions(@PathVariable("studentId") Long studentId) {
        return ResponseEntity.ok(interventionService.getInterventionsByStudent(studentId));
    }

    @Auditable(action = "READ_STUDENT_INTERVENTIONS_SLICE", entityName = "StudentIntervention", entityId = "#studentId")
    @GetMapping("/student/{studentId}/slice")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUIDANCE', 'STUDENT_AFFAIRS') or @enrollmentSecurity.canAccessStudentAdvising(authentication, #studentId)")
    public ResponseEntity<SliceResponse<StudentInterventionDto>> getStudentInterventionsSlice(
            @PathVariable("studentId") Long studentId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(interventionService.getInterventionsByStudentSlice(
                studentId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dispatchedAt"))));
    }

    @Auditable(action = "READ_ALL_INTERVENTIONS", entityName = "StudentIntervention")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'GUIDANCE', 'STUDENT_AFFAIRS')")
    public ResponseEntity<Page<StudentInterventionDto>> getAllInterventions(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(interventionService.getAllInterventions(
                status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dispatchedAt"))));
    }
}
