package com.sdt.web_app.websocket.dto;

import java.time.Instant;

/**
 * Broadcast to /topic/performance.{studentId} when student performance or risk metrics update.
 */
public record PerformanceSummaryMessage(
        Long studentProfileId,
        Long termId,
        Double gpa,
        String academicStanding,
        String riskLevel,
        Instant updatedAt
) {
    public Long studentId() {
        return studentProfileId;
    }
}
