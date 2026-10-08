package com.sdt.web_app.websocket.dto;

import java.time.Instant;

/**
 * Broadcast to /topic/grades.{studentId} when student grades are updated or finalized.
 */
public record GradeUpdateMessage(
        Long studentProfileId,
        Long classSectionId,
        String courseCode,
        Double midtermGrade,
        Double finalGrade,
        Double computedFinalGrade,
        String remarks,
        Instant updatedAt
) {
    public Long studentId() {
        return studentProfileId;
    }
}
