package com.sdt.web_app.websocket.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Broadcast to /topic/attendance.{studentId} and /topic/attendance.class.{classId} when attendance is logged.
 */
public record AttendanceUpdateMessage(
        Long studentProfileId,
        Long classSectionId,
        LocalDate attendanceDate,
        String status,
        String remarks,
        Instant timestamp
) {
    public Long studentId() {
        return studentProfileId;
    }
}
