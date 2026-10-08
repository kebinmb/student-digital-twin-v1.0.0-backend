package com.sdt.web_app.websocket.dto;

import java.time.Instant;

/**
 * Broadcast to /topic/equity.{studentId} when student equity classification changes.
 */
public record EquityProfileMessage(
        Long studentProfileId,
        String classification,
        String verificationStatus,
        Double incomeCap,
        String verifiedBy,
        Instant updatedAt
) {
    public Long studentId() {
        return studentProfileId;
    }
}
