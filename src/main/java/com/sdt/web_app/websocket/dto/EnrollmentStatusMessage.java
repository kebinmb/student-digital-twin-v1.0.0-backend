package com.sdt.web_app.websocket.dto;

import java.time.Instant;

/**
 * Broadcast to /topic/enrollment.{studentId} when enrollment state changes.
 */
public record EnrollmentStatusMessage(
        Long enrollmentId,
        Long studentProfileId,
        Long termId,
        String enrollmentStatus,
        String remarks,
        Instant updatedAt
) {}
