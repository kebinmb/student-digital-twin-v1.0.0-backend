package com.sdt.web_app.controller.compliance;

import com.sdt.web_app.annotation.Auditable;
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
    @Auditable(action = "INITIATE_CLEARANCE", entityName = "ClearanceRequest")
    @PostMapping("/clearance/requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'STUDENT')")
    public ResponseEntity<ClearanceRequestDto> initiateClearanceRequest(@Valid @RequestBody InitiateClearanceRequest request, Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        ClearanceRequestDto result = clearanceWorkflowService.initiateClearanceRequest(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Auditable(action = "READ_CLEARANCE_BY_STUDENT_AND_TERM", entityName = "ClearanceRequest")
    @GetMapping({"/clearance/requests/student/{studentIdentifier}/term/{termId}", "/clearance/student/{studentIdentifier}/term/{termId}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'CASHIER', 'ACCOUNTANT', 'GUIDANCE', 'FACULTY', 'STUDENT')")
    public ResponseEntity<ClearanceRequestDto> getClearanceByStudentAndTerm(@PathVariable String studentIdentifier, @PathVariable Long termId) {
        ClearanceRequestDto result = clearanceWorkflowService.getClearanceByStudentAndTerm(studentIdentifier, termId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "READ_CLEARANCE_BY_ID", entityName = "ClearanceRequest", entityId = "#id")
    @GetMapping("/clearance/requests/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'CASHIER', 'ACCOUNTANT', 'GUIDANCE', 'FACULTY', 'STUDENT')")
    public ResponseEntity<ClearanceRequestDto> getClearanceById(@PathVariable Long id) {
        ClearanceRequestDto result = clearanceWorkflowService.getClearanceById(id);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "CLEARANCE_SIGNOFF", entityName = "ClearanceSignoff", entityId = "#signoffId")
    @PutMapping("/clearance/signoffs/{signoffId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CASHIER', 'ACCOUNTANT', 'GUIDANCE', 'CHAIRPERSON', 'FACULTY')")
    public ResponseEntity<ClearanceSignoffDto> processSignoff(
            @PathVariable Long signoffId,
            @Valid @RequestBody ProcessSignoffRequest request,
            Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        ClearanceSignoffDto result = clearanceWorkflowService.processSignoff(signoffId, request, actorUserId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "READ_PENDING_SIGNOFFS", entityName = "ClearanceSignoff")
    @GetMapping("/clearance/signoffs/pending/{departmentType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CASHIER', 'ACCOUNTANT', 'GUIDANCE', 'CHAIRPERSON', 'FACULTY')")
    public ResponseEntity<List<ClearanceSignoffDto>> getPendingSignoffsByDepartment(@PathVariable String departmentType) {
        List<ClearanceSignoffDto> result = clearanceWorkflowService.getPendingSignoffsByDepartment(departmentType);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "READ_CLEARANCE_STUDENT_SUGGESTIONS", entityName = "ClearanceRequest")
    @GetMapping("/clearance/requests/students/suggestions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'CASHIER', 'ACCOUNTANT', 'GUIDANCE', 'FACULTY')")
    public ResponseEntity<List<ClearanceStudentSuggestionDto>> getClearanceStudentSuggestions(
            @RequestParam(value = "query", required = false, defaultValue = "") String query) {
        List<ClearanceStudentSuggestionDto> result = clearanceWorkflowService.getClearanceStudentSuggestions(query);
        return ResponseEntity.ok(result);
    }

    // Degree Audit & Graduation
    @Auditable(action = "EVALUATE_DEGREE_AUDIT", entityName = "DegreeAudit", entityId = "#studentProfileId")
    @GetMapping("/graduation/audit/{studentProfileId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON') or @enrollmentSecurity.canAccessStudentAdvising(authentication, #studentProfileId)")
    public ResponseEntity<DegreeAuditResultDto> evaluateDegreeAudit(@PathVariable Long studentProfileId) {
        DegreeAuditResultDto result = degreeAuditService.evaluateDegreeAudit(studentProfileId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "APPLY_GRADUATION", entityName = "GraduationApplication")
    @PostMapping("/graduation/apply")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'STUDENT')")
    public ResponseEntity<GraduationApplicationDto> applyForGraduation(@Valid @RequestBody ApplyForGraduationRequest request, Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        GraduationApplicationDto result = degreeAuditService.applyForGraduation(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Auditable(action = "ISSUE_SPECIAL_ORDER", entityName = "GraduationApplication", entityId = "#id")
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

    @Auditable(action = "READ_GRADUATION_APPLICATIONS_BY_TERM", entityName = "GraduationApplication", entityId = "#termId")
    @GetMapping("/graduation/applications/term/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN')")
    public ResponseEntity<List<GraduationApplicationDto>> getGraduationApplicationsByTerm(@PathVariable Long termId) {
        List<GraduationApplicationDto> result = degreeAuditService.getGraduationApplicationsByTerm(termId);
        return ResponseEntity.ok(result);
    }

    // CHED Regulatory Reporting
    @Auditable(action = "EXPORT_CHED_FORM_E1", entityName = "ChedFormE1", entityId = "#campusId")
    @GetMapping("/compliance/ched/e1/{campusId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN')")
    public ResponseEntity<ChedFormE1InstitutionalDto> exportFormE1Institutional(@PathVariable Long campusId) {
        ChedFormE1InstitutionalDto result = chedHemisExportService.exportFormE1Institutional(campusId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "EXPORT_CHED_FORM_E2", entityName = "ChedFormE2", entityId = "#campusId")
    @GetMapping("/compliance/ched/e2/{campusId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN')")
    public ResponseEntity<List<ChedFormE2ProgramDto>> exportFormE2Programs(@PathVariable Long campusId) {
        List<ChedFormE2ProgramDto> result = chedHemisExportService.exportFormE2Programs(campusId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "EXPORT_CHED_FORM_E3", entityName = "ChedFormE3", entityId = "#termId")
    @GetMapping("/compliance/ched/e3/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN')")
    public ResponseEntity<List<ChedFormE3EnrolmentDto>> exportFormE3Enrolment(@PathVariable Long termId) {
        List<ChedFormE3EnrolmentDto> result = chedHemisExportService.exportFormE3Enrolment(termId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "EXPORT_CHED_FORM_E4", entityName = "ChedFormE4", entityId = "#termId")
    @GetMapping("/compliance/ched/e4/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN')")
    public ResponseEntity<List<ChedFormE4GraduateDto>> exportFormE4Graduates(@PathVariable Long termId) {
        List<ChedFormE4GraduateDto> result = chedHemisExportService.exportFormE4Graduates(termId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "EXPORT_CHED_FORM_E5", entityName = "ChedFormE5", entityId = "#termId")
    @GetMapping("/compliance/ched/e5/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN')")
    public ResponseEntity<List<ChedFormE5FacultyDto>> exportFormE5Faculty(@PathVariable Long termId) {
        List<ChedFormE5FacultyDto> result = chedHemisExportService.exportFormE5Faculty(termId);
        return ResponseEntity.ok(result);
    }
}
