package com.sdt.web_app.controller.grade;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.grade.GradeChangeDtos.*;
import com.sdt.web_app.service.grade.GradeChangeService;
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
@RequestMapping("/api/v1/grades/change-requests")
@RequiredArgsConstructor
public class GradeChangeRequestController {

    private final GradeChangeService gradeChangeService;
    private final SecurityUtils securityUtils;

    @Auditable(action = "SUBMIT_GRADE_CHANGE_REQUEST", entityName = "GradeChangeRequest")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<GradeChangeResponse> submitRequest(
            @Valid @RequestBody CreateGradeChangeRequest request,
            Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(gradeChangeService.submitRequest(request, actorUserId));
    }

    @Auditable(action = "READ_PENDING_GRADE_CHANGE_REQUESTS", entityName = "GradeChangeRequest")
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<List<GradeChangeResponse>> getPendingRequests() {
        return ResponseEntity.ok(gradeChangeService.getPendingRequests());
    }

    @Auditable(action = "APPROVE_GRADE_CHANGE_REQUEST", entityName = "GradeChangeRequest", entityId = "#id")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<GradeChangeResponse> approveRequest(
            @PathVariable("id") Long id,
            Authentication authentication) {
        Long approverUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(gradeChangeService.approveRequest(id, approverUserId));
    }

    @Auditable(action = "REJECT_GRADE_CHANGE_REQUEST", entityName = "GradeChangeRequest", entityId = "#id")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<GradeChangeResponse> rejectRequest(
            @PathVariable("id") Long id,
            Authentication authentication) {
        Long rejectorUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(gradeChangeService.rejectRequest(id, rejectorUserId));
    }
}
