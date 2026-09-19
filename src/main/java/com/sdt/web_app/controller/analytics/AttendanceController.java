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

    @PostMapping("/session/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<AttendanceSessionResponse> startSession(@Valid @RequestBody StartAttendanceSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.startSession(request));
    }

    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT', 'FACULTY')")
    public ResponseEntity<AttendanceRecordResponse> scanAttendance(@Valid @RequestBody ScanAttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.scanAttendance(request));
    }

    @GetMapping("/student/{studentId}/slice")
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
}
