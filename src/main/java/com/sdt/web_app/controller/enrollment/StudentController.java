package com.sdt.web_app.controller.enrollment;

import com.sdt.web_app.dto.enrollment.EnrollmentDtos.*;
import com.sdt.web_app.service.enrollment.StudentService;
import com.sdt.web_app.service.enrollment.TransfereeCreditingService;
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
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final TransfereeCreditingService creditingService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<StudentProfileResponse> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        StudentProfileResponse response = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY') or @enrollmentSecurity.canAccessStudentAdvising(authentication, #id)")
    public ResponseEntity<StudentProfileResponse> getStudentById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY')")
    public ResponseEntity<List<StudentSearchResultDto>> searchStudents(
            @RequestParam(value = "query", required = false, defaultValue = "") String query) {
        return ResponseEntity.ok(studentService.searchStudents(query));
    }

    @PostMapping("/{id}/credit-courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<TransfereeCreditingSummaryResponse> creditTransfereeCourses(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreditTransfereeCoursesRequest request,
            Authentication authentication) {
        Long approverUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(creditingService.creditTransfereeCourses(id, request, approverUserId));
    }

    @GetMapping("/{id}/credited-courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY') or @enrollmentSecurity.canAccessStudentAdvising(authentication, #id)")
    public ResponseEntity<List<CourseEquivalencyDto>> getCreditedCourses(@PathVariable("id") Long id) {
        return ResponseEntity.ok(creditingService.getStudentCourseEquivalencies(id));
    }
}
