package com.sdt.web_app.controller.enrollment;

import com.sdt.web_app.dto.enrollment.EnrollmentDtos.*;
import com.sdt.web_app.service.enrollment.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/enrollment")
@RequiredArgsConstructor
@Validated
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // -------------------------------------------------------------------------
    // Gate 3: Student Advising & Eligibility
    // -------------------------------------------------------------------------
    @GetMapping("/advising/student/{studentId}/term/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<AdvisingEligibilityResponse> getAdvisingEligibility(
            @PathVariable("studentId") Long studentId,
            @PathVariable("termId") Long termId,
            @RequestParam(value = "targetYearLevel", required = false) Integer targetYearLevel,
            @RequestParam(value = "targetSemester", required = false) String targetSemester) {
        return ResponseEntity.ok(enrollmentService.getAdvisingEligibility(studentId, termId, targetYearLevel, targetSemester));
    }

    // -------------------------------------------------------------------------
    // Section Enlistment (Atomic Capacity Check & Unit Ceiling Guard)
    // -------------------------------------------------------------------------
    @PostMapping("/enlist/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'STUDENT')")
    public ResponseEntity<StudentEnrollmentResponse> enlistSection(
            @PathVariable("studentId") Long studentId,
            @Valid @RequestBody EnlistSectionRequest request) {
        return ResponseEntity.ok(enrollmentService.enlistSection(studentId, request));
    }

    @DeleteMapping("/enlist/student/{studentId}/term/{termId}/section/{sectionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'STUDENT')")
    public ResponseEntity<StudentEnrollmentResponse> removeEnlistedSection(
            @PathVariable("studentId") Long studentId,
            @PathVariable("termId") Long termId,
            @PathVariable("sectionId") Long sectionId) {
        return ResponseEntity.ok(enrollmentService.removeEnlistedSection(studentId, termId, sectionId));
    }

    @PostMapping("/confirm/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'STUDENT')")
    public ResponseEntity<EnrollmentConfirmationDto> confirmEnrollment(
            @PathVariable("studentId") Long studentId,
            @Valid @RequestBody ConfirmEnrollmentRequest request) {
        return ResponseEntity.ok(enrollmentService.confirmEnrollment(studentId, request));
    }

    @GetMapping("/student/{studentId}/term/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'STUDENT')")
    public ResponseEntity<StudentEnrollmentResponse> getEnrollment(
            @PathVariable("studentId") Long studentId,
            @PathVariable("termId") Long termId) {
        return ResponseEntity.ok(enrollmentService.getEnrollment(studentId, termId));
    }

    // -------------------------------------------------------------------------
    // Registrar & Admin Audit & Oversight Endpoints
    // -------------------------------------------------------------------------
    @GetMapping("/term/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<java.util.List<StudentEnrollmentResponse>> getEnrollmentsByTerm(
            @PathVariable("termId") Long termId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByTerm(termId));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<StudentEnrollmentResponse> updateEnrollmentStatus(
            @PathVariable("id") Long enrollmentId,
            @Valid @RequestBody UpdateEnrollmentStatusRequest request) {
        return ResponseEntity.ok(enrollmentService.updateEnrollmentStatus(enrollmentId, request));
    }
}
