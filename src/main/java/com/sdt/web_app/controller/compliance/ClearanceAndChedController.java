package com.sdt.web_app.controller.compliance;

import com.sdt.web_app.dto.compliance.ComplianceDtos.*;
import com.sdt.web_app.service.compliance.ChedHemisExportService;
import com.sdt.web_app.service.compliance.ClearanceWorkflowService;
import com.sdt.web_app.service.compliance.DegreeAuditService;
import com.sdt.web_app.service.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClearanceAndChedController {

    private final ClearanceWorkflowService clearanceWorkflowService;
    private final DegreeAuditService degreeAuditService;
    private final ChedHemisExportService chedHemisExportService;
    private final SecurityUtils securityUtils;

    // Clearance Workflows
    @PostMapping("/clearance/requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'STUDENT')")
    public ResponseEntity<ClearanceRequestDto> initiateClearanceRequest(@Valid @RequestBody InitiateClearanceRequest request, Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        ClearanceRequestDto result = clearanceWorkflowService.initiateClearanceRequest(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/clearance/requests/student/{studentProfileId}/term/{termId}")
    public ResponseEntity<ClearanceRequestDto> getClearanceByStudentAndTerm(@PathVariable Long studentProfileId, @PathVariable Long termId) {
        ClearanceRequestDto result = clearanceWorkflowService.getClearanceByStudentAndTerm(studentProfileId, termId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/clearance/requests/{id}")
    public ResponseEntity<ClearanceRequestDto> getClearanceById(@PathVariable Long id) {
        ClearanceRequestDto result = clearanceWorkflowService.getClearanceById(id);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/clearance/signoffs/{signoffId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN', 'ACCOUNTANT')")
    public ResponseEntity<ClearanceSignoffDto> processSignoff(
            @PathVariable Long signoffId,
            @Valid @RequestBody ProcessSignoffRequest request,
            Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        ClearanceSignoffDto result = clearanceWorkflowService.processSignoff(signoffId, request, actorUserId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/clearance/signoffs/pending/{departmentType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN', 'ACCOUNTANT')")
    public ResponseEntity<List<ClearanceSignoffDto>> getPendingSignoffsByDepartment(@PathVariable String departmentType) {
        List<ClearanceSignoffDto> result = clearanceWorkflowService.getPendingSignoffsByDepartment(departmentType);
        return ResponseEntity.ok(result);
    }

    // Degree Audit & Graduation
    @GetMapping("/graduation/audit/{studentProfileId}")
    public ResponseEntity<DegreeAuditResultDto> evaluateDegreeAudit(@PathVariable Long studentProfileId) {
        DegreeAuditResultDto result = degreeAuditService.evaluateDegreeAudit(studentProfileId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/graduation/apply")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'STUDENT')")
    public ResponseEntity<GraduationApplicationDto> applyForGraduation(@Valid @RequestBody ApplyForGraduationRequest request, Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        GraduationApplicationDto result = degreeAuditService.applyForGraduation(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/graduation/applications/{id}/special-order")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<GraduationApplicationDto> issueSpecialOrder(
            @PathVariable Long id,
            @Valid @RequestBody IssueSpecialOrderRequest request,
            Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        GraduationApplicationDto result = degreeAuditService.issueSpecialOrder(id, request.specialOrderNumber(), actorUserId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/graduation/applications/term/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN')")
    public ResponseEntity<List<GraduationApplicationDto>> getGraduationApplicationsByTerm(@PathVariable Long termId) {
        List<GraduationApplicationDto> result = degreeAuditService.getGraduationApplicationsByTerm(termId);
        return ResponseEntity.ok(result);
    }

    // CHED Regulatory Reporting
    @GetMapping("/compliance/ched/e1/{campusId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN')")
    public ResponseEntity<ChedFormE1InstitutionalDto> exportFormE1Institutional(@PathVariable Long campusId) {
        ChedFormE1InstitutionalDto result = chedHemisExportService.exportFormE1Institutional(campusId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/compliance/ched/e3/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN')")
    public ResponseEntity<List<ChedFormE3EnrolmentDto>> exportFormE3Enrolment(@PathVariable Long termId) {
        List<ChedFormE3EnrolmentDto> result = chedHemisExportService.exportFormE3Enrolment(termId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/compliance/ched/e4/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN')")
    public ResponseEntity<List<ChedFormE4GraduateDto>> exportFormE4Graduates(@PathVariable Long termId) {
        List<ChedFormE4GraduateDto> result = chedHemisExportService.exportFormE4Graduates(termId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/compliance/ched/e5/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN')")
    public ResponseEntity<List<ChedFormE5FacultyDto>> exportFormE5Faculty(@PathVariable Long termId) {
        List<ChedFormE5FacultyDto> result = chedHemisExportService.exportFormE5Faculty(termId);
        return ResponseEntity.ok(result);
    }
}
