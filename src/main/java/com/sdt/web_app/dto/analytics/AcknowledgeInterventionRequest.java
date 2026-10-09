package com.sdt.web_app.dto.analytics;

/**
 * Request body for POST /api/v1/student/telemetry/interventions/{id}/acknowledge
 * Sent by the student (or staff) to acknowledge and respond to an intervention.
 */
public record AcknowledgeInterventionRequest(
        String response
) {
    public AcknowledgeInterventionRequest {
        if (response != null && response.isBlank()) {
            response = null;
        }
    }
}
