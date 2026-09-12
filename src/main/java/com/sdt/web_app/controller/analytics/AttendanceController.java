package com.sdt.web_app.controller.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.*;
import com.sdt.web_app.service.analytics.QrAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
