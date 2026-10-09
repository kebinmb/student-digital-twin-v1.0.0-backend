package com.sdt.web_app.controller.enrollment;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.admission.AdmissionDtos.AdmissionApplicationResponse;
import com.sdt.web_app.dto.enrollment.EnrollmentDtos.*;
import com.sdt.web_app.service.admission.AdmissionService;
import com.sdt.web_app.service.enrollment.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollment")
@RequiredArgsConstructor
@Validated
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final AdmissionService admissionService;
    private final com.sdt.web_app.repositories.enrollment.StudentProfileRepository studentProfileRepository;
    private final com.sdt.web_app.service.security.SecurityUtils securityUtils;

    @Auditable(action = "READ_APPROVED_ADMISSIONS", entityName = "AdmissionApplication")
    @GetMapping("/admissions/approved")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<List<AdmissionApplicationResponse>> getApprovedAdmissions(
            @RequestParam(value = "termId", required = false) Long termId) {
        return ResponseEntity.ok(admissionService.getUnclaimedApprovedApplications(termId));
    }

    // -------------------------------------------------------------------------
    // Gate 3: Student Advising & Eligibility
    // -------------------------------------------------------------------------
    @Auditable(action = "READ_ADVISING_ELIGIBILITY", entityName = "StudentEnrollment", entityId = "#studentId")
    @GetMapping("/advising/student/{studentId}/term/{termId}")
    @PreAuthorize("@enrollmentSecurity.canAccessStudentAdvising(authentication, #studentId)")
    public ResponseEntity<AdvisingEligibilityResponse> getAdvisingEligibility(
            @PathVariable("studentId") Long studentId,
            @PathVariable("termId") Long termId,
            @RequestParam(value = "targetYearLevel", required = false) Integer targetYearLevel,
            @RequestParam(value = "targetSemester", required = false) String targetSemester,
            @RequestParam(value = "allCourses", required = false, defaultValue = "false") Boolean allCourses) {
        return ResponseEntity.ok(enrollmentService.getAdvisingEligibility(studentId, termId, targetYearLevel, targetSemester, allCourses));
    }

    // -------------------------------------------------------------------------
    // Section Enlistment (Atomic Capacity Check & Unit Ceiling Guard)
    // -------------------------------------------------------------------------
    @Auditable(action = "ENLIST_SECTION", entityName = "StudentEnrollment", entityId = "#studentId")
    @PostMapping("/enlist/student/{studentId}")
    @PreAuthorize("@enrollmentSecurity.canAccessStudentEnrollment(authentication, #studentId)")
    public ResponseEntity<StudentEnrollmentResponse> enlistSection(
            @PathVariable("studentId") Long studentId,
            @Valid @RequestBody EnlistSectionRequest request) {
        return ResponseEntity.ok(enrollmentService.enlistSection(studentId, request));
    }

    @Auditable(action = "DROP_SECTION", entityName = "EnrollmentCourseItem", entityId = "#sectionId")
    @DeleteMapping("/enlist/student/{studentId}/term/{termId}/section/{sectionId}")
    @PreAuthorize("@enrollmentSecurity.canAccessStudentEnrollment(authentication, #studentId)")
    public ResponseEntity<StudentEnrollmentResponse> removeEnlistedSection(
            @PathVariable("studentId") Long studentId,
            @PathVariable("termId") Long termId,
            @PathVariable("sectionId") Long sectionId) {
        return ResponseEntity.ok(enrollmentService.removeEnlistedSection(studentId, termId, sectionId));
    }

    @Auditable(action = "CONFIRM_ENROLLMENT", entityName = "StudentEnrollment", entityId = "#studentId")
    @PostMapping("/confirm/student/{studentId}")
    @PreAuthorize("@enrollmentSecurity.canAccessStudentEnrollment(authentication, #studentId)")
    public ResponseEntity<EnrollmentConfirmationDto> confirmEnrollment(
            @PathVariable("studentId") Long studentId,
            @Valid @RequestBody ConfirmEnrollmentRequest request) {
        return ResponseEntity.ok(enrollmentService.confirmEnrollment(studentId, request));
    }

    @Auditable(action = "READ_ENROLLMENT", entityName = "StudentEnrollment", entityId = "#studentId")
    @GetMapping("/student/{studentId}/term/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'CASHIER') or @enrollmentSecurity.canAccessStudentEnrollment(authentication, #studentId)")
    public ResponseEntity<StudentEnrollmentResponse> getEnrollment(
            @PathVariable("studentId") Long studentId,
            @PathVariable("termId") Long termId) {
        return ResponseEntity.ok(enrollmentService.getEnrollment(studentId, termId));
    }

    @Auditable(action = "READ_MY_ENROLLMENT", entityName = "StudentEnrollment")
    @GetMapping("/me/term/{termId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentEnrollmentResponse> getMyEnrollment(
            @PathVariable("termId") Long termId,
            org.springframework.security.core.Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        if (userId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Unable to resolve user identity.");
        }
        com.sdt.web_app.entities.enrollment.StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Student profile not found for user: " + userId));
        return ResponseEntity.ok(enrollmentService.getEnrollment(profile.getId(), termId));
    }

    // -------------------------------------------------------------------------
    // Registrar & Admin Audit & Oversight Endpoints
    // -------------------------------------------------------------------------
    @Auditable(action = "READ_ENROLLMENTS_BY_TERM", entityName = "StudentEnrollment", entityId = "#termId")
    @GetMapping("/term/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<java.util.List<StudentEnrollmentResponse>> getEnrollmentsByTerm(
            @PathVariable("termId") Long termId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByTerm(termId));
    }

    @Auditable(action = "UPDATE_ENROLLMENT_STATUS", entityName = "StudentEnrollment", entityId = "#enrollmentId")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<StudentEnrollmentResponse> updateEnrollmentStatus(
            @PathVariable("id") Long enrollmentId,
            @Valid @RequestBody UpdateEnrollmentStatusRequest request) {
        return ResponseEntity.ok(enrollmentService.updateEnrollmentStatus(enrollmentId, request));
    }
}
