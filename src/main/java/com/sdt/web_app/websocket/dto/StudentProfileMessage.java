package com.sdt.web_app.websocket.dto;

import java.time.Instant;

/**
 * Broadcast to /topic/student.{studentId} when student profile attributes change.
 */
public record StudentProfileMessage(
        Long studentProfileId,
        Long userId,
        String studentNumber,
        String firstName,
        String lastName,
        String email,
        String enrollmentStatus,
        Long currentTermId,
        Instant updatedAt
) {
    public Long studentId() {
        return studentProfileId;
    }
}
