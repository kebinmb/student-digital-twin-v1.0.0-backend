package com.sdt.web_app.websocket.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Broadcast to /topic/clearance.{studentId} when student clearance status changes.
 */
public record ClearanceStatusMessage(
        Long studentId,
        Long termId,
        String overallStatus,
        List<DepartmentClearanceItem> departments
) {
    public Long studentProfileId() {
        return studentId;
    }

    public record DepartmentClearanceItem(
            Long departmentId,
            String departmentName,
            String status,
            String remarks,
            String clearedBy,
            LocalDateTime clearedAt
    ) {}
}
