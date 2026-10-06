package com.sdt.web_app.controller.institution;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.institution.TermDtos.*;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.service.institution.TermLifecycleService;
import com.sdt.web_app.service.institution.TermService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;
    private final TermLifecycleService termLifecycleService;
    private final com.sdt.web_app.service.institution.HonorRollComputationService honorRollService;
    private final com.sdt.web_app.service.institution.HonorRollCertificateService certificateService;

    @Auditable(action = "READ_ALL_TERMS", entityName = "Term")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<List<TermResponse>> getAllTerms() {
        List<TermResponse> responses = termService.getAllTerms().stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Auditable(action = "READ_ACTIVE_TERM", entityName = "Term")
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<TermResponse> getActiveTerm() {
        return ResponseEntity.ok(mapToResponse(termService.getActiveTerm()));
    }

    @Auditable(action = "READ_TERMS_BY_ACADEMIC_YEAR", entityName = "Term", entityId = "#academicYearId")
    @GetMapping("/academic-year/{academicYearId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<List<TermResponse>> getTermsByAcademicYear(@PathVariable Long academicYearId) {
        List<TermResponse> responses = termService.getTermsByAcademicYear(academicYearId).stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Auditable(action = "READ_TERM", entityName = "Term", entityId = "#id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<TermResponse> getTermById(@PathVariable Long id) {
        return ResponseEntity.ok(mapToResponse(termService.getTermById(id)));
    }

    @Auditable(action = "CREATE_TERM", entityName = "Term")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<TermResponse> createTerm(@Valid @RequestBody CreateTermRequest request) {
        Term term = termService.createTerm(
                request.academicYearId(),
                request.termType(),
                request.startDate(),
                request.endDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(term));
    }

    @Auditable(action = "UPDATE_TERM_SCHEDULE", entityName = "Term", entityId = "#id")
    @PutMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<TermResponse> updateTermSchedule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTermScheduleRequest request
    ) {
        Term term = termService.updateTermSchedule(id, request.startDate(), request.endDate());
        return ResponseEntity.ok(mapToResponse(term));
    }

    @Auditable(action = "ACTIVATE_TERM", entityName = "Term", entityId = "#id")
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<TermResponse> activateTerm(@PathVariable Long id) {
        Term term = termLifecycleService.activateTerm(id);
        return ResponseEntity.ok(mapToResponse(term));
    }

    @Auditable(action = "UPDATE_ENROLLMENT_WINDOW", entityName = "Term", entityId = "#id")
    @PutMapping("/{id}/enrollment-window")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<TermResponse> toggleEnrollmentWindow(
            @PathVariable Long id,
            @RequestParam boolean open
    ) {
        Term term = open ? termLifecycleService.openEnrollment(id) : termLifecycleService.closeEnrollment(id);
        return ResponseEntity.ok(mapToResponse(term));
    }

    @Auditable(action = "UPDATE_GRADING_WINDOW", entityName = "Term", entityId = "#id")
    @PutMapping("/{id}/grading-window")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<TermResponse> toggleGradingWindow(
            @PathVariable Long id,
            @RequestParam boolean open
    ) {
        Term term = open ? termLifecycleService.openGrading(id) : termLifecycleService.lockGrading(id);
        return ResponseEntity.ok(mapToResponse(term));
    }

    @Auditable(action = "UPDATE_ADD_DROP_WINDOW", entityName = "Term", entityId = "#id")
    @PutMapping("/{id}/add-drop-window")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<TermResponse> toggleAddDropWindow(
            @PathVariable Long id,
            @RequestParam boolean open
    ) {
        Term term = termLifecycleService.toggleAddDrop(id, open);
        return ResponseEntity.ok(mapToResponse(term));
    }

    @Auditable(action = "READ_TERM_HONOR_ROLL", entityName = "Term", entityId = "#id")
    @GetMapping("/{id}/honor-roll")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<com.sdt.web_app.dto.institution.HonorRollDtos.TermHonorRollReportDto> getTermHonorRoll(
            @PathVariable Long id,
            @RequestParam(required = false) Long programId
    ) {
        return ResponseEntity.ok(honorRollService.computeTermHonorRoll(id, programId));
    }

    @Auditable(action = "READ_HONOR_CERTIFICATE", entityName = "Term", entityId = "#id")
    @GetMapping("/{id}/honor-certificate/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<com.sdt.web_app.dto.institution.HonorRollDtos.CertificateVerificationDto> getHonorCertificate(
            @PathVariable Long id,
            @PathVariable Long studentId
    ) {
        return ResponseEntity.ok(certificateService.generateCertificateMetadata(id, studentId));
    }

    @Auditable(action = "EXPORT_BATCH_HONOR_CERTIFICATES", entityName = "Term", entityId = "#id")
    @GetMapping("/{id}/honor-certificates/zip")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<byte[]> downloadTermCertificatesZip(
            @PathVariable Long id,
            @RequestParam(required = false) Long programId
    ) {
        byte[] zipBytes = certificateService.generateTermCertificatesZip(id, programId);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Term_" + id + "_Honor_Certificates.zip\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);
    }

    @Auditable(action = "DELETE_TERM", entityName = "Term", entityId = "#id")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTerm(@PathVariable Long id) {
        termService.deleteTerm(id);
        return ResponseEntity.noContent().build();
    }

    private TermResponse mapToResponse(Term t) {
        return new TermResponse(
                t.getId(),
                t.getAcademicYear().getId(),
                t.getAcademicYear().getCode(),
                t.getTermType(),
                t.getStartDate(),
                t.getEndDate(),
                t.getAcademicYear().isCurrent(),
                t.isActive(),
                t.isEnrollmentOpen(),
                t.isGradingOpen(),
                t.isAddDropOpen()
        );
    }
}
