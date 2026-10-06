package com.sdt.web_app.controller.lms;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.lms.LmsDtos.StudentSelfServiceSummaryDto;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.service.lms.StudentNotificationPublisherService;
import com.sdt.web_app.service.lms.StudentPortalService;
import com.sdt.web_app.service.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/students/portal")
@RequiredArgsConstructor
public class StudentPortalController {

    private final StudentPortalService portalService;
    private final StudentNotificationPublisherService notificationService;
    private final SecurityUtils securityUtils;
    private final StudentProfileRepository studentProfileRepository;

    @Auditable(action = "READ_STUDENT_PORTAL_SUMMARY", entityName = "StudentPortalSummary", entityId = "#studentId")
    @GetMapping("/summary/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<StudentSelfServiceSummaryDto> getStudentPortalSummary(@PathVariable("studentId") Long studentId) {
        return ResponseEntity.ok(portalService.getStudentPortalSummary(studentId));
    }

    @Auditable(action = "READ_STUDENT_PORTAL_SELF", entityName = "StudentPortalSummary")
    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentSelfServiceSummaryDto> getMyStudentPortalSummary(Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        if (userId == null) {
            throw new AccessDeniedException("Unable to resolve authenticated student user identity.");
        }
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found for user: " + userId));
        return ResponseEntity.ok(portalService.getStudentPortalSummary(profile.getId()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'REGISTRAR', 'DEAN')")
    public SseEmitter streamStudentNotifications(
            @RequestParam(value = "studentId", required = false) Long studentId,
            Authentication authentication) {
        Long resolvedStudentId = studentId;

        Long userId = securityUtils.resolveUserId(authentication);
        boolean isStudent = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        if (isStudent && userId != null) {
            StudentProfile profile = studentProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new EntityNotFoundException("Student profile not found for user: " + userId));
            // Ensure student role cannot listen to another student's events
            resolvedStudentId = profile.getId();
        }

        if (resolvedStudentId == null) {
            throw new IllegalArgumentException("Target studentId could not be resolved for stream.");
        }

        return notificationService.subscribeToStudentEvents(resolvedStudentId);
    }
}
