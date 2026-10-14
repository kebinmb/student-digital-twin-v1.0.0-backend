package com.sdt.web_app.controller.scheduling;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.scheduling.SchedulingOverrideDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.service.scheduling.SchedulingOverrideService;
import com.sdt.web_app.service.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/scheduling/overrides")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SchedulingOverrideController {

    private final SchedulingOverrideService overrideService;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;

    @Auditable(action = "VALIDATE_SCHEDULING_ASSIGNMENT", entityName = "ClassSection", entityId = "#sectionId")
    @GetMapping("/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<OverrideValidationResponse> validateAssignment(
            @RequestParam("sectionId") Long sectionId,
            @RequestParam("facultyUserId") Long facultyUserId) {
        return ResponseEntity.ok(overrideService.validateAssignment(sectionId, facultyUserId));
    }

    @Auditable(action = "CREATE_SCHEDULING_OVERRIDE_REQUEST", entityName = "SchedulingOverrideRequest", entityId = "#request.sectionId()")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<SchedulingOverrideResponse> createOverrideRequest(
            @Valid @RequestBody CreateOverrideRequest request,
            Authentication authentication) {
        Long currentUserId = securityUtils.resolveUserId(authentication);
        if (currentUserId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Unable to resolve authenticated user identity.");
        }
        SchedulingOverrideResponse response = overrideService.createOverrideRequest(
                request.sectionId(),
                request.scheduleId(),
                request.facultyUserId(),
                request.justification(),
                currentUserId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Auditable(action = "READ_SCHEDULING_OVERRIDE_INBOX", entityName = "SchedulingOverrideRequest")
    @GetMapping("/inbox")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'FACULTY')")
    public ResponseEntity<List<SchedulingOverrideResponse>> getInbox(Authentication authentication) {
        Long currentUserId = securityUtils.resolveUserId(authentication);
        if (currentUserId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Unable to resolve authenticated user identity.");
        }
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + currentUserId));
        return ResponseEntity.ok(overrideService.getInbox(currentUser));
    }

    @Auditable(action = "READ_PENDING_OVERRIDE_COUNT", entityName = "SchedulingOverrideRequest")
    @GetMapping("/pending-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'FACULTY')")
    public ResponseEntity<PendingOverrideCountResponse> getPendingCount(Authentication authentication) {
        Long currentUserId = securityUtils.resolveUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.ok(new PendingOverrideCountResponse(0));
        }
        return ResponseEntity.ok(new PendingOverrideCountResponse(overrideService.getPendingCount(currentUserId)));
    }

    @Auditable(action = "READ_SCHEDULING_OVERRIDE_BY_SECTION", entityName = "ClassSection", entityId = "#sectionId")
    @GetMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'FACULTY')")
    public ResponseEntity<List<SchedulingOverrideResponse>> getOverridesBySection(@PathVariable("sectionId") Long sectionId) {
        return ResponseEntity.ok(overrideService.getRequestsBySection(sectionId));
    }

    @Auditable(action = "READ_SCHEDULING_OVERRIDE_DETAILS", entityName = "SchedulingOverrideRequest", entityId = "#id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'FACULTY')")
    public ResponseEntity<SchedulingOverrideResponse> getOverrideById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(overrideService.getRequestById(id));
    }

    @Auditable(action = "APPROVE_SCHEDULING_OVERRIDE_STEP", entityName = "SchedulingOverrideApproval", entityId = "#approvalId")
    @PostMapping("/{requestId}/approvals/{approvalId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'FACULTY')")
    public ResponseEntity<SchedulingOverrideResponse> approveStep(
            @PathVariable("requestId") Long requestId,
            @PathVariable("approvalId") Long approvalId,
            @Valid @RequestBody ApprovalDecisionRequest request,
            Authentication authentication) {
        Long currentUserId = securityUtils.resolveUserId(authentication);
        if (currentUserId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Unable to resolve authenticated user identity.");
        }
        SchedulingOverrideResponse response = overrideService.approveStep(
                requestId, approvalId, currentUserId, request.remarks());
        return ResponseEntity.ok(response);
    }

    @Auditable(action = "REJECT_SCHEDULING_OVERRIDE_STEP", entityName = "SchedulingOverrideApproval", entityId = "#approvalId")
    @PostMapping("/{requestId}/approvals/{approvalId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'FACULTY')")
    public ResponseEntity<SchedulingOverrideResponse> rejectStep(
            @PathVariable("requestId") Long requestId,
            @PathVariable("approvalId") Long approvalId,
            @Valid @RequestBody ApprovalDecisionRequest request,
            Authentication authentication) {
        Long currentUserId = securityUtils.resolveUserId(authentication);
        if (currentUserId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Unable to resolve authenticated user identity.");
        }
        SchedulingOverrideResponse response = overrideService.rejectStep(
                requestId, approvalId, currentUserId, request.remarks());
        return ResponseEntity.ok(response);
    }

    @Auditable(action = "CANCEL_SCHEDULING_OVERRIDE_REQUEST", entityName = "SchedulingOverrideRequest", entityId = "#id")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<SchedulingOverrideResponse> cancelRequest(
            @PathVariable("id") Long id,
            @Valid @RequestBody ApprovalDecisionRequest request,
            Authentication authentication) {
        Long currentUserId = securityUtils.resolveUserId(authentication);
        if (currentUserId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Unable to resolve authenticated user identity.");
        }
        SchedulingOverrideResponse response = overrideService.cancelRequest(
                id, currentUserId, request.remarks());
        return ResponseEntity.ok(response);
    }
}
