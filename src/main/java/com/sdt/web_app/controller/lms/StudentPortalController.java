package com.sdt.web_app.controller.lms;

import com.sdt.web_app.dto.lms.LmsDtos.StudentSelfServiceSummaryDto;
import com.sdt.web_app.service.lms.StudentPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students/portal")
@RequiredArgsConstructor
public class StudentPortalController {

    private final StudentPortalService portalService;

    @GetMapping("/summary/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<StudentSelfServiceSummaryDto> getStudentPortalSummary(@PathVariable("studentId") Long studentId) {
        return ResponseEntity.ok(portalService.getStudentPortalSummary(studentId));
    }
}
