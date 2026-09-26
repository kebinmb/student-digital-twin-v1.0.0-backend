package com.sdt.web_app.controller.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.*;
import com.sdt.web_app.service.analytics.QrAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.sdt.web_app.dto.common.SliceResponse;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final QrAttendanceService attendanceService;
    private final com.sdt.web_app.service.enrollment.StudentService studentService;
    private final com.sdt.web_app.service.security.SecurityUtils securityUtils;

    @PostMapping("/session/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<AttendanceSessionResponse> startSession(
            @Valid @RequestBody StartAttendanceSessionRequest request,
            org.springframework.security.core.Authentication authentication) {
        Long creatorUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.startSession(request, creatorUserId));
    }

    @PostMapping("/creator/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<FacultyAttendanceRecordResponse> verifyCreatorAttendance(
            @Valid @RequestBody VerifyCreatorAttendanceRequest request,
            org.springframework.security.core.Authentication authentication) {
        Long creatorUserId = securityUtils.resolveUserId(authentication);
        if (creatorUserId == null) {
            throw new IllegalStateException("Cannot resolve authenticated instructor/admin identity.");
        }
        return ResponseEntity.ok(attendanceService.verifyCreatorAttendance(request, creatorUserId));
    }

    @GetMapping("/creator/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<java.util.List<FacultyAttendanceRecordResponse>> getDailyFacultyAttendance(
            @RequestParam(name = "date", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @RequestParam(name = "sectionId", required = false) Long sectionId) {
        return ResponseEntity.ok(attendanceService.getDailyFacultyAttendance(date, sectionId));
    }

    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT', 'FACULTY')")
    public ResponseEntity<AttendanceRecordResponse> scanAttendance(
            @Valid @RequestBody ScanAttendanceRequest request,
            org.springframework.security.core.Authentication authentication) {
        Long resolvedStudentId = request.studentId();
        boolean isStudent = authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (isStudent) {
            Long userId = securityUtils.resolveUserId(authentication);
            if (userId != null) {
                try {
                    com.sdt.web_app.dto.enrollment.EnrollmentDtos.StudentProfileResponse sp = studentService.getStudentByUserId(userId);
                    if (sp != null && sp.id() != null) {
                        resolvedStudentId = sp.id();
                    }
                } catch (Exception ignored) {
                    // Fall back to request.studentId()
                }
            }
        } else {
            Long currentUserId = securityUtils.resolveUserId(authentication);
            // Non-student (admin/faculty) checking in themselves should be recorded in faculty_attendance_records
            if (currentUserId != null && currentUserId.equals(request.studentId())) {
                FacultyAttendanceRecordResponse creatorResp = attendanceService.verifyCreatorAttendance(
                        new VerifyCreatorAttendanceRequest(request.qrSeed(), request.latitude(), request.longitude(), request.deviceFingerprint()),
                        currentUserId
                );
                return ResponseEntity.ok(new AttendanceRecordResponse(
                        creatorResp.recordId(),
                        creatorResp.sessionId(),
                        creatorResp.sectionCode(),
                        creatorResp.courseCode(),
                        creatorResp.facultyUserId(),
                        "FACULTY-HOST",
                        creatorResp.facultyName(),
                        creatorResp.attendanceStatus(),
                        creatorResp.isGeofenceValid(),
                        creatorResp.verifiedAt(),
                        creatorResp.deviceFingerprint()
                ));
            }
        }
        ScanAttendanceRequest effectiveRequest = (resolvedStudentId != null && !resolvedStudentId.equals(request.studentId()))
                ? new ScanAttendanceRequest(request.qrSeed(), resolvedStudentId, request.latitude(), request.longitude(), request.deviceFingerprint())
                : request;
        return ResponseEntity.ok(attendanceService.scanAttendance(effectiveRequest));
    }

    @GetMapping("/student/me/slice")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SliceResponse<AttendanceRecordResponse>> getCurrentStudentAttendanceSlice(
            org.springframework.security.core.Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "DESC") String sortDir) {
        Long userId = securityUtils.resolveUserId(authentication);
        if (userId == null) {
            throw new IllegalStateException("Cannot resolve authenticated student user identity.");
        }
        com.sdt.web_app.dto.enrollment.EnrollmentDtos.StudentProfileResponse student = studentService.getStudentByUserId(userId);
        return ResponseEntity.ok(attendanceService.getStudentAttendanceSlice(student.id(), page, size, sortBy, sortDir));
    }

    @GetMapping("/student/{studentId:[0-9]+}/slice")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<SliceResponse<AttendanceRecordResponse>> getStudentAttendanceSlice(
            @PathVariable("studentId") Long studentId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "DESC") String sortDir) {
        return ResponseEntity.ok(attendanceService.getStudentAttendanceSlice(studentId, page, size, sortBy, sortDir));
    }

    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<java.util.List<AttendanceRecordResponse>> getDailyAttendance(
            @RequestParam(name = "date", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @RequestParam(name = "sectionId", required = false) Long sectionId) {
        return ResponseEntity.ok(attendanceService.getDailyAttendance(date, sectionId));
    }

    @GetMapping(value = "/stream/{sessionId}", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'DEAN', 'CHAIRPERSON', 'STUDENT')")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamSessionAttendance(@PathVariable("sessionId") Long sessionId) {
        return attendanceService.subscribeToSessionStream(sessionId);
    }
}
