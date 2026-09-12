package com.sdt.web_app.controller.grade;

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

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<GradeChangeResponse> submitRequest(
            @Valid @RequestBody CreateGradeChangeRequest request,
            Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(gradeChangeService.submitRequest(request, actorUserId));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<List<GradeChangeResponse>> getPendingRequests() {
        return ResponseEntity.ok(gradeChangeService.getPendingRequests());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<GradeChangeResponse> approveRequest(
            @PathVariable("id") Long id,
            Authentication authentication) {
        Long approverUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(gradeChangeService.approveRequest(id, approverUserId));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<GradeChangeResponse> rejectRequest(
            @PathVariable("id") Long id,
            Authentication authentication) {
        Long rejectorUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(gradeChangeService.rejectRequest(id, rejectorUserId));
    }
}
