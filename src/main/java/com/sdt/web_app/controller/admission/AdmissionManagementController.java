package com.sdt.web_app.controller.admission;

import com.sdt.web_app.dto.admission.AdmissionDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.service.admission.AdmissionService;
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
@RequestMapping({"/api/v1/admission", "/api/admission"})
@RequiredArgsConstructor
public class AdmissionManagementController {

    private final AdmissionService admissionService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<AdmissionConfigDto> getAdmissionConfig(
            @RequestParam(value = "termId", required = false) Long termId) {
        return ResponseEntity.ok(admissionService.getAdmissionConfig(termId));
    }

    @PutMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<AdmissionConfigDto> updateAdmissionConfig(
            @Valid @RequestBody UpdateAdmissionConfigRequest request) {
        return ResponseEntity.ok(admissionService.updateAdmissionConfig(request));
    }

    @GetMapping("/exam-slots")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<List<EntranceExamSlotResponse>> getAllExamSlots(
            @RequestParam(value = "termId", required = false) Long termId) {
        return ResponseEntity.ok(admissionService.getAllExamSlotsForAdmin(termId));
    }

    @PostMapping("/exam-slots")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<EntranceExamSlotResponse> createExamSlot(
            @Valid @RequestBody CreateExamSlotRequest request) {
        EntranceExamSlotResponse created = admissionService.createExamSlot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/exam-slots/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<EntranceExamSlotResponse> updateExamSlotStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status) {
        EntranceExamSlotResponse updated = admissionService.updateExamSlotStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/exam-slots/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<Void> deleteExamSlot(@PathVariable("id") Long id) {
        admissionService.deleteExamSlot(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<List<AdmissionApplicationResponse>> getAllApplications(
            @RequestParam(value = "termId", required = false) Long termId,
            @RequestParam(value = "status", required = false) String status) {
        List<AdmissionApplicationResponse> apps = admissionService.getAllApplications(termId, status);
        return ResponseEntity.ok(apps);
    }

    @GetMapping("/applications/program/{programId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<List<AdmissionApplicationResponse>> getApplicationsForProgram(
            @PathVariable("programId") Long programId,
            @RequestParam(value = "status", required = false) String status) {
        List<AdmissionApplicationResponse> apps = admissionService.getApplicationsForProgram(programId, status);
        return ResponseEntity.ok(apps);
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<AdmissionApplicationResponse> getApplicationById(@PathVariable("id") Long id) {
        List<AdmissionApplicationResponse> apps = admissionService.getAllApplications(null, null);
        AdmissionApplicationResponse app = apps.stream()
                .filter(a -> a.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Admission Application not found with ID: " + id));
        return ResponseEntity.ok(app);
    }

    @PostMapping("/applications/{id}/evaluate-exam")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<AdmissionApplicationResponse> evaluateExam(
            @PathVariable("id") Long id,
            @Valid @RequestBody EvaluateExamRequest request,
            Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        User currentUser = userId != null ? userRepository.findById(userId).orElse(null) : null;
        AdmissionApplicationResponse updated = admissionService.evaluateExam(id, request, currentUser);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/applications/{id}/evaluate-interview")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<AdmissionApplicationResponse> evaluateInterview(
            @PathVariable("id") Long id,
            @Valid @RequestBody EvaluateInterviewRequest request,
            Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        User currentUser = userId != null ? userRepository.findById(userId).orElse(null) : null;
        AdmissionApplicationResponse updated = admissionService.evaluateInterview(id, request, currentUser);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/applications/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<AdmissionApplicationResponse> updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAdmissionStatusRequest request) {
        AdmissionApplicationResponse updated = admissionService.updateApplicationStatus(id, request);
        return ResponseEntity.ok(updated);
    }
}
