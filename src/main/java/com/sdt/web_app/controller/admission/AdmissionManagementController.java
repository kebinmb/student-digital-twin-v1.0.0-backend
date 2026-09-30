package com.sdt.web_app.controller.admission;

import com.sdt.web_app.annotation.Auditable;
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
    private final com.sdt.web_app.service.security.DataScopingService dataScopingService;

    @Auditable(action = "READ_CONFIG", entityName = "AdmissionConfig")
    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<AdmissionConfigDto> getAdmissionConfig(
            @RequestParam(value = "termId", required = false) Long termId) {
        return ResponseEntity.ok(admissionService.getAdmissionConfig(termId));
    }

    @Auditable(action = "UPDATE_CONFIG", entityName = "AdmissionConfig")
    @PutMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<AdmissionConfigDto> updateAdmissionConfig(
            @Valid @RequestBody UpdateAdmissionConfigRequest request) {
        return ResponseEntity.ok(admissionService.updateAdmissionConfig(request));
    }

    @Auditable(action = "READ_ALL_EXAM_SLOTS", entityName = "EntranceExamSlot")
    @GetMapping("/exam-slots")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<List<EntranceExamSlotResponse>> getAllExamSlots(
            @RequestParam(value = "termId", required = false) Long termId) {
        return ResponseEntity.ok(admissionService.getAllExamSlotsForAdmin(termId));
    }

    @Auditable(action = "CREATE_EXAM_SLOT", entityName = "EntranceExamSlot")
    @PostMapping("/exam-slots")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<EntranceExamSlotResponse> createExamSlot(
            @Valid @RequestBody CreateExamSlotRequest request) {
        EntranceExamSlotResponse created = admissionService.createExamSlot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Auditable(action = "UPDATE_EXAM_SLOT_STATUS", entityName = "EntranceExamSlot", entityId = "#id")
    @PutMapping("/exam-slots/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<EntranceExamSlotResponse> updateExamSlotStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status) {
        EntranceExamSlotResponse updated = admissionService.updateExamSlotStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @Auditable(action = "DELETE_EXAM_SLOT", entityName = "EntranceExamSlot", entityId = "#id")
    @DeleteMapping("/exam-slots/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<Void> deleteExamSlot(@PathVariable("id") Long id) {
        admissionService.deleteExamSlot(id);
        return ResponseEntity.noContent().build();
    }

    @Auditable(action = "READ_ALL_APPLICATIONS", entityName = "AdmissionApplication")
    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<List<AdmissionApplicationResponse>> getAllApplications(
            @RequestParam(value = "termId", required = false) Long termId,
            @RequestParam(value = "status", required = false) String status,
            Authentication authentication) {
        java.util.Optional<List<Long>> scopedPrograms = dataScopingService.getScopedProgramIds(authentication);
        List<AdmissionApplicationResponse> apps = admissionService.getAllApplications(termId, status, scopedPrograms);
        return ResponseEntity.ok(apps);
    }

    @Auditable(action = "READ_UNCLAIMED_APPLICATIONS", entityName = "AdmissionApplication")
    @GetMapping("/applications/unclaimed")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<List<AdmissionApplicationResponse>> getUnclaimedApplications(
            @RequestParam(value = "termId", required = false) Long termId) {
        List<AdmissionApplicationResponse> apps = admissionService.getUnclaimedApprovedApplications(termId);
        return ResponseEntity.ok(apps);
    }

    @Auditable(action = "READ_PROGRAM_APPLICATIONS", entityName = "AdmissionApplication", entityId = "#programId")
    @GetMapping("/applications/program/{programId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<List<AdmissionApplicationResponse>> getApplicationsForProgram(
            @PathVariable("programId") Long programId,
            @RequestParam(value = "status", required = false) String status,
            Authentication authentication) {
        java.util.Optional<List<Long>> scopedPrograms = dataScopingService.getScopedProgramIds(authentication);
        if (scopedPrograms.isPresent() && !scopedPrograms.get().contains(programId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to applications for program ID: " + programId);
        }
        List<AdmissionApplicationResponse> apps = admissionService.getApplicationsForProgram(programId, status);
        return ResponseEntity.ok(apps);
    }

    @Auditable(action = "READ_APPLICATION", entityName = "AdmissionApplication", entityId = "#id")
    @GetMapping("/applications/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<AdmissionApplicationResponse> getApplicationById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(admissionService.getApplicationById(id));
    }

    @Auditable(action = "EVALUATE_EXAM", entityName = "AdmissionApplication", entityId = "#id")
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

    @Auditable(action = "EVALUATE_INTERVIEW", entityName = "AdmissionApplication", entityId = "#id")
    @PostMapping("/applications/{id}/evaluate-interview")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<AdmissionApplicationResponse> evaluateInterview(
            @PathVariable("id") Long id,
            @Valid @RequestBody EvaluateInterviewRequest request,
            Authentication authentication) {
        java.util.Optional<List<Long>> scopedPrograms = dataScopingService.getScopedProgramIds(authentication);
        if (scopedPrograms.isPresent()) {
            AdmissionApplicationResponse existingApp = admissionService.getApplicationById(id);
            if (existingApp.targetProgramId() != null && !scopedPrograms.get().contains(existingApp.targetProgramId())) {
                throw new org.springframework.security.access.AccessDeniedException("Chairperson cannot evaluate interview for an applicant outside assigned program scope.");
            }
        }
        Long userId = securityUtils.resolveUserId(authentication);
        User currentUser = userId != null ? userRepository.findById(userId).orElse(null) : null;
        AdmissionApplicationResponse updated = admissionService.evaluateInterview(id, request, currentUser);
        return ResponseEntity.ok(updated);
    }

    @Auditable(action = "UPDATE_STATUS", entityName = "AdmissionApplication", entityId = "#id")
    @PutMapping("/applications/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'GUIDANCE')")
    public ResponseEntity<AdmissionApplicationResponse> updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAdmissionStatusRequest request) {
        AdmissionApplicationResponse updated = admissionService.updateApplicationStatus(id, request);
        return ResponseEntity.ok(updated);
    }
}
